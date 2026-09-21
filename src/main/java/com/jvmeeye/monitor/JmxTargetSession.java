package com.jvmeeye.monitor;

import com.jvmeeye.monitor.dto.ClassInfo;
import com.jvmeeye.monitor.dto.CompilationInfo;
import com.jvmeeye.monitor.dto.CpuInfo;
import com.jvmeeye.monitor.dto.GcCollectorInfo;
import com.jvmeeye.monitor.dto.GcInfo;
import com.jvmeeye.monitor.dto.LastGcInfo;
import com.jvmeeye.monitor.dto.MemoryInfo;
import com.jvmeeye.monitor.dto.MemoryPoolInfo;
import com.jvmeeye.monitor.dto.MetricsSnapshot;
import com.jvmeeye.monitor.dto.RuntimeInfo;
import com.jvmeeye.monitor.dto.ThreadInfo;
import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 单个目标 JVM 的监控会话:持有 JMX 连接、缓存各 MBean 代理,并维护有界环形历史缓冲。
 *
 * <p>线程安全:采样由单一定时任务驱动,查询方法只读。</p>
 */
@Slf4j
public class JmxTargetSession implements AutoCloseable {

    /** DiagnosticCommand MBean 对象名(jcmd 能力:类直方图、线程打印等)。 */
    public static final ObjectName DIAGNOSTIC_COMMAND =
            createObjectName("com.sun.management:type=DiagnosticCommand");

    private final long pid;
    private final String displayName;
    private final JMXConnector connector;
    private final MBeanServerConnection connection;
    private final BlockingQueue<MetricsSnapshot> history;
    private final long connectedAt;

    private final MemoryMXBean memoryMXBean;
    private final List<com.sun.management.GarbageCollectorMXBean> sunGcMXBeans;
    private final List<MemoryPoolMXBean> poolMXBeans;
    private final ThreadMXBean threadMXBean;
    private final OperatingSystemMXBean osMXBean;
    private final ClassLoadingMXBean classLoadingMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final CompilationMXBean compilationMXBean;

    private volatile boolean closed = false;
    private volatile String closeReason = null;

    public JmxTargetSession(long pid, String displayName, JMXConnector connector, int historySize) throws IOException {
        this.pid = pid;
        this.displayName = displayName;
        this.connector = connector;
        this.connection = connector.getMBeanServerConnection();
        this.history = new ArrayBlockingQueue<>(Math.max(2, historySize));
        this.connectedAt = System.currentTimeMillis();

        this.memoryMXBean = ManagementFactory.newPlatformMXBeanProxy(connection,
                ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        this.sunGcMXBeans = new ArrayList<>();
        for (com.sun.management.GarbageCollectorMXBean bean : ManagementFactory.getPlatformMXBeans(connection,
                com.sun.management.GarbageCollectorMXBean.class)) {
            sunGcMXBeans.add(bean);
        }
        this.poolMXBeans = ManagementFactory.getPlatformMXBeans(connection, MemoryPoolMXBean.class);
        this.threadMXBean = ManagementFactory.newPlatformMXBeanProxy(connection,
                ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        this.osMXBean = ManagementFactory.newPlatformMXBeanProxy(connection,
                ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        this.classLoadingMXBean = ManagementFactory.newPlatformMXBeanProxy(connection,
                ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
        this.runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(connection,
                ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
        CompilationMXBean compilation = null;
        try {
            compilation = ManagementFactory.newPlatformMXBeanProxy(connection,
                    ManagementFactory.COMPILATION_MXBEAN_NAME, CompilationMXBean.class);
        } catch (Exception e) {
            log.debug("pid={} no compilation mxbean", pid);
        }
        this.compilationMXBean = compilation;
    }

    public long getPid() {
        return pid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }

    public ThreadMXBean getThreadMXBean() {
        return threadMXBean;
    }

    public ObjectName getDiagnosticCommandName() {
        return DIAGNOSTIC_COMMAND;
    }

    /** 采集一次完整指标快照(不入缓冲)。 */
    public MetricsSnapshot collect() throws IOException {
        RuntimeInfo runtime = collectRuntime();
        MemoryInfo memory = collectMemory();
        GcInfo gc = collectGc();
        ThreadInfo thread = collectThreads();
        CpuInfo cpu = collectCpu();
        ClassInfo classLoading = collectClasses();
        CompilationInfo compilation = collectCompilation();
        return new MetricsSnapshot(System.currentTimeMillis(), pid, displayName,
                runtime, memory, gc, thread, cpu, classLoading, compilation);
    }

    /** 采集并写入历史缓冲;缓冲满时丢弃最旧的一点。 */
    public MetricsSnapshot sample() throws IOException {
        MetricsSnapshot snapshot = collect();
        if (!history.offer(snapshot)) {
            history.poll();
            history.offer(snapshot);
        }
        return snapshot;
    }

    /** 取最近 n 点历史(按时间升序)。 */
    public List<MetricsSnapshot> history(int n) {
        MetricsSnapshot[] all = history.toArray(new MetricsSnapshot[0]);
        int size = Math.min(Math.max(1, n), all.length);
        List<MetricsSnapshot> out = new ArrayList<>(size);
        for (int i = all.length - size; i < all.length; i++) {
            out.add(all[i]);
        }
        return out;
    }

    public int historySize() {
        return history.size();
    }

    private RuntimeInfo collectRuntime() throws IOException {
        RuntimeMXBean bean = runtimeMXBean;
        long uptime = bean.getUptime();
        List<String> args = new ArrayList<>(bean.getInputArguments());
        return new RuntimeInfo(
                bean.getVmName(),
                bean.getVmVersion(),
                bean.getVmVendor(),
                bean.getSpecVersion(),
                bean.getName(),
                bean.getVmVendor(),
                bean.getManagementSpecVersion(),
                bean.getStartTime(),
                uptime,
                formatDuration(uptime),
                args);
    }

    private MemoryInfo collectMemory() throws IOException {
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        List<MemoryPoolInfo> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : poolMXBeans) {
            MemoryUsage usage;
            try {
                usage = pool.getUsage();
            } catch (Exception e) {
                usage = null;
            }
            MemoryUsage peak = pool.getPeakUsage();
            String threshold = null;
            try {
                if (pool.isUsageThresholdSupported() && pool.isUsageThresholdExceeded()) {
                    threshold = "exceeded";
                }
            } catch (Exception ignored) {
                // best effort
            }
            pools.add(new MemoryPoolInfo(
                    pool.getName(),
                    pool.getType().name(),
                    usage == null ? -1 : usage.getUsed(),
                    usage == null ? -1 : usage.getCommitted(),
                    usage == null ? -1 : usage.getMax(),
                    peak == null ? -1 : peak.getUsed(),
                    peak == null ? -1 : peak.getMax(),
                    threshold));
        }
        double percent = heap.getMax() > 0 ? (double) heap.getUsed() / heap.getMax() : -1d;
        return new MemoryInfo(
                heap.getUsed(), heap.getCommitted(), heap.getMax(), heap.getInit(), percent,
                nonHeap.getUsed(), nonHeap.getCommitted(), nonHeap.getMax(),
                memoryMXBean.getObjectPendingFinalizationCount(),
                pools);
    }

    private GcInfo collectGc() throws IOException {
        List<GcCollectorInfo> collectors = new ArrayList<>();
        long totalCount = 0;
        long totalTime = 0;
        LastGcInfo last = null;
        for (int i = 0; i < sunGcMXBeans.size(); i++) {
            com.sun.management.GarbageCollectorMXBean bean = sunGcMXBeans.get(i);
            long count = bean.getCollectionCount();
            if (count < 0) {
                count = 0;
            }
            long time = bean.getCollectionTime();
            if (time < 0) {
                time = 0;
            }
            collectors.add(new GcCollectorInfo(bean.getName(), count, time));
            totalCount += count;
            totalTime += time;
            try {
                com.sun.management.GcInfo info = bean.getLastGcInfo();
                if (info != null) {
                    LastGcInfo candidate = new LastGcInfo(bean.getName(), info.getId(),
                            info.getStartTime(), info.getEndTime(), info.getDuration());
                    if (last == null || candidate.endTime() > last.endTime()) {
                        last = candidate;
                    }
                }
            } catch (Exception e) {
                log.trace("pid={} getLastGcInfo failed: {}", pid, e.toString());
            }
        }
        return new GcInfo(collectors, totalCount, totalTime, last);
    }

    private ThreadInfo collectThreads() throws IOException {
        int live = threadMXBean.getThreadCount();
        int daemon = threadMXBean.getDaemonThreadCount();
        int peak = threadMXBean.getPeakThreadCount();
        long totalStarted = threadMXBean.getTotalStartedThreadCount();

        Map<String, Integer> states = new LinkedHashMap<>();
        for (Thread.State state : Thread.State.values()) {
            states.put(state.name(), 0);
        }
        long[] ids = threadMXBean.getAllThreadIds();
        if (ids != null && ids.length > 0) {
            for (java.lang.management.ThreadInfo info : threadMXBean.getThreadInfo(ids, 0)) {
                if (info != null) {
                    states.merge(info.getThreadState().name(), 1, Integer::sum);
                }
            }
        }

        long[] deadlocked = null;
        try {
            deadlocked = threadMXBean.findDeadlockedThreads();
        } catch (Exception e) {
            log.trace("pid={} findDeadlockedThreads failed: {}", pid, e.toString());
        }
        List<Long> deadlockedIds = new ArrayList<>();
        if (deadlocked != null) {
            for (long id : deadlocked) {
                deadlockedIds.add(id);
            }
        }

        boolean cpuTimeSupported = threadMXBean.isCurrentThreadCpuTimeSupported();
        return new ThreadInfo(
                live, daemon, peak, totalStarted,
                cpuTimeSupported ? threadMXBean.getCurrentThreadCpuTime() / 1_000_000 : -1,
                cpuTimeSupported ? threadMXBean.getCurrentThreadUserTime() / 1_000_000 : -1,
                states,
                !deadlockedIds.isEmpty(),
                deadlockedIds.size(),
                deadlockedIds);
    }

    private CpuInfo collectCpu() throws IOException {
        return new CpuInfo(
                osMXBean.getProcessCpuLoad(),
                osMXBean.getSystemCpuLoad(),
                osMXBean.getSystemLoadAverage(),
                osMXBean.getAvailableProcessors(),
                osMXBean.getProcessCpuTime() / 1_000_000);
    }

    private ClassInfo collectClasses() throws IOException {
        return new ClassInfo(
                classLoadingMXBean.getLoadedClassCount(),
                classLoadingMXBean.getTotalLoadedClassCount(),
                classLoadingMXBean.getUnloadedClassCount(),
                classLoadingMXBean.isVerbose());
    }

    private CompilationInfo collectCompilation() throws IOException {
        if (compilationMXBean == null) {
            return null;
        }
        return new CompilationInfo(compilationMXBean.getName(), compilationMXBean.getTotalCompilationTime());
    }

    private static String formatDuration(long ms) {
        Duration duration = Duration.ofMillis(ms);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        if (days > 0) {
            return String.format("%d天%02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static ObjectName createObjectName(String name) {
        try {
            return new ObjectName(name);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            connector.close();
        } catch (IOException e) {
            log.debug("close connector pid={} failed: {}", pid, e.toString());
        }
    }

    public void markClosed(String reason) {
        this.closed = true;
        this.closeReason = reason;
    }
}
