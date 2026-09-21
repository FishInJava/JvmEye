package com.jvmeeye.diagnostics;

import com.jvmeeye.monitor.JmxConnectionManager;
import com.jvmeeye.monitor.JmxTargetSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 线程 Dump 服务。
 *
 * <p>通过 {@link java.lang.management.ThreadMXBean#dumpAllThreads(boolean, boolean)}
 * 获取完整堆栈,并结合 {@code findDeadlockedThreads()} 标记死锁线程。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThreadDumpService {

    private final JmxConnectionManager connectionManager;

    public ThreadDump dump() {
        JmxTargetSession session = connectionManager.active();
        try {
            return dump(session);
        } catch (IOException e) {
            session.markClosed("线程 dump 失败: " + e.getMessage());
            throw new com.jvmeeye.exception.ApiException(409,
                    "目标 JVM 连接已断开(pid=" + session.getPid() + "): " + e.getMessage(), e);
        }
    }

    public ThreadDump dump(JmxTargetSession session) throws IOException {
        java.lang.management.ThreadMXBean bean = session.getThreadMXBean();
        long timestamp = System.currentTimeMillis();
        ThreadInfo[] infos = bean.dumpAllThreads(true, true);
        long[] deadlocked = bean.findDeadlockedThreads();
        long[] monitorDeadlocked = bean.findMonitorDeadlockedThreads();
        Set<Long> deadlockedIds = new LinkedHashSet<>();
        if (deadlocked != null) {
            for (long id : deadlocked) {
                deadlockedIds.add(id);
            }
        }
        if (monitorDeadlocked != null) {
            for (long id : monitorDeadlocked) {
                deadlockedIds.add(id);
            }
        }

        List<ThreadDump.Thread> threads = new ArrayList<>(infos.length);
        int blockedByDeadlock = 0;
        for (ThreadInfo info : infos) {
            List<ThreadDump.StackFrame> stack = new ArrayList<>();
            for (StackTraceElement element : info.getStackTrace()) {
                stack.add(new ThreadDump.StackFrame(
                        element.getClassName(),
                        element.getMethodName(),
                        element.getFileName(),
                        element.getLineNumber(),
                        element.isNativeMethod()));
            }
            List<ThreadDump.Lock> lockedMonitors = new ArrayList<>();
            for (MonitorInfo monitor : info.getLockedMonitors()) {
                lockedMonitors.add(new ThreadDump.Lock(
                        monitor.getClassName() + "#" + monitor.getIdentityHashCode(),
                        monitor.getLockedStackDepth(),
                        monitor.getLockedStackFrame() == null ? null
                                : monitor.getLockedStackFrame().toString()));
            }
            List<ThreadDump.Lock> lockedSynchronizers = new ArrayList<>();
            for (LockInfo lock : info.getLockedSynchronizers()) {
                lockedSynchronizers.add(new ThreadDump.Lock(lock.toString(), -1, null));
            }
            String lockName = info.getLockName();
            String lockOwner = info.getLockOwnerName();
            ThreadDump.Thread thread = new ThreadDump.Thread(
                    info.getThreadId(),
                    info.getThreadName(),
                    info.getThreadState().name(),
                    info.getPriority(),
                    info.isDaemon(),
                    info.isSuspended(),
                    info.isInNative(),
                    info.getBlockedCount(),
                    info.getBlockedTime(),
                    info.getWaitedCount(),
                    info.getWaitedTime(),
                    lockName,
                    lockOwner,
                    deadlockedIds.contains(info.getThreadId()),
                    stack,
                    lockedMonitors,
                    lockedSynchronizers);
            if (thread.deadlocked()) {
                blockedByDeadlock++;
            }
            threads.add(thread);
        }
        return new ThreadDump(timestamp, session.getPid(), session.getDisplayName(),
                threads.size(), blockedByDeadlock, new ArrayList<>(deadlockedIds), threads);
    }

    /** 线程 Dump 结构(record 形式便于 JSON 序列化)。 */
    public record ThreadDump(long timestamp, long pid, String displayName, int threadCount,
                             int deadlockedCount, List<Long> deadlockedThreadIds,
                             List<Thread> threads) {

        public record Thread(long id, String name, String state, int priority, boolean daemon,
                             boolean suspended, boolean inNative, long blockedCount, long blockedTimeMs,
                             long waitedCount, long waitedTimeMs, String lockName, String lockOwnerName,
                             boolean deadlocked, List<StackFrame> stack,
                             List<Lock> lockedMonitors, List<Lock> lockedSynchronizers) {
        }

        public record StackFrame(String className, String methodName, String fileName,
                                 int lineNumber, boolean nativeMethod) {
        }

        public record Lock(String identity, int stackDepth, String stackFrame) {
        }
    }
}
