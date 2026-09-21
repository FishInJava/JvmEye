package com.jvmeeye.monitor;

import com.jvmeeye.config.JvmEyeProperties;
import com.jvmeeye.exception.NoActiveTargetException;
import com.jvmeeye.exception.TargetNotAttachableException;
import com.sun.tools.attach.VirtualMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 目标连接管理。
 *
 * <p>v1 默认单目标:连接新目标时释放已有连接;内部结构使用 {@link ConcurrentHashMap},
 * 打开 {@code jvmeeye.monitor.multi-target=true} 即可支持多目标并发(前端仍按单目标展示)。</p>
 */
@Component
@Slf4j
public class JmxConnectionManager {

    private final Map<Long, JmxTargetSession> sessions = new ConcurrentHashMap<>();
    private final JvmEyeProperties properties;

    /** 最近一次连接的 pid,作为“当前活动目标”。 */
    private volatile long activePid = -1L;

    public JmxConnectionManager(JvmEyeProperties properties) {
        this.properties = properties;
    }

    /**
     * 连接到指定 pid 的 JVM。
     *
     * @param pid 目标进程号
     * @return 会话
     */
    public synchronized JmxTargetSession connect(long pid) {
        JmxTargetSession existing = sessions.get(pid);
        if (existing != null && !existing.isClosed()) {
            activePid = pid;
            return existing;
        }
        if (existing != null) {
            sessions.remove(pid, existing);
            existing.close();
        }

        if (!properties.getMonitor().isMultiTarget()) {
            for (JmxTargetSession session : new ArrayList<>(sessions.values())) {
                sessions.remove(session.getPid(), session);
                session.close();
                log.info("released previous target pid={} (single-target mode)", session.getPid());
            }
        }

        VirtualMachine vm = null;
        JMXConnector connector = null;
        try {
            vm = VirtualMachine.attach(Long.toString(pid));
            String address = vm.getAgentProperties()
                    .getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (address == null) {
                vm.startLocalManagementAgent();
                address = vm.getAgentProperties()
                        .getProperty("com.sun.management.jmxremote.localConnectorAddress");
            }
            if (address == null) {
                throw new TargetNotAttachableException("目标 JVM 未暴露 localConnectorAddress(pid=" + pid + ")");
            }
            connector = JMXConnectorFactory.connect(new JMXServiceURL(address), null);
            String displayName = vm.getSystemProperties().getProperty("sun.java.command");
            JmxTargetSession session = new JmxTargetSession(pid, displayName, connector,
                    properties.getMonitor().getHistorySize());
            sessions.put(pid, session);
            activePid = pid;
            log.info("connected to pid={} command={}", pid, displayName);
            return session;
        } catch (TargetNotAttachableException e) {
            closeQuietly(connector);
            detachQuietly(vm);
            throw e;
        } catch (Exception e) {
            closeQuietly(connector);
            detachQuietly(vm);
            log.warn("connect to pid={} failed: {}", pid, e.toString());
            throw new TargetNotAttachableException("连接目标 JVM 失败(pid=" + pid + "): " + e.getMessage(), e);
        } finally {
            // attach 句柄仅用于开启本地管理 agent,可以立即 detach
            detachQuietly(vm);
        }
    }

    /** 当前活动目标(没有则抛 409)。 */
    public JmxTargetSession active() {
        JmxTargetSession session = activeOrNull();
        if (session == null) {
            throw new NoActiveTargetException("当前没有已连接的监控目标,请先选择目标连接");
        }
        return session;
    }

    public JmxTargetSession activeOrNull() {
        JmxTargetSession session = sessions.get(activePid);
        if (session != null && session.isClosed()) {
            sessions.remove(activePid, session);
            if (activePid == session.getPid()) {
                activePid = -1L;
            }
            return null;
        }
        return session;
    }

    public List<JmxTargetSession> all() {
        List<JmxTargetSession> list = new ArrayList<>(sessions.values());
        list.sort(Comparator.comparingLong(JmxTargetSession::getPid));
        return list;
    }

    /** 断开指定目标;pid 为 null 时断开全部。 */
    public synchronized int disconnect(Long pid) {
        if (pid == null) {
            int count = sessions.size();
            for (JmxTargetSession session : new ArrayList<>(sessions.values())) {
                sessions.remove(session.getPid(), session);
                session.close();
            }
            activePid = -1L;
            log.info("disconnected all targets ({} closed)", count);
            return count;
        }
        JmxTargetSession session = sessions.remove(pid);
        if (session == null) {
            return 0;
        }
        session.close();
        if (activePid == pid) {
            activePid = -1L;
        }
        log.info("disconnected target pid={}", pid);
        return 1;
    }

    /**
     * 清理已失效的连接(目标进程退出 / JMX 断开)。由采样任务调用。
     *
     * @return 被清理的 pid 列表
     */
    public List<Long> evictDeadSessions() {
        List<Long> removed = new ArrayList<>();
        for (JmxTargetSession session : new ArrayList<>(sessions.values())) {
            if (session.isClosed() || !isAlive(session)) {
                if (sessions.remove(session.getPid(), session)) {
                    removed.add(session.getPid());
                }
                session.close();
            }
        }
        if (!removed.isEmpty() && activePid != -1L && removed.contains(activePid)) {
            activePid = -1L;
        }
        return removed;
    }

    private boolean isAlive(JmxTargetSession session) {
        try {
            session.getConnection().getMBeanCount();
            return true;
        } catch (IOException e) {
            session.markClosed("JMX 连接已断开: " + e.getMessage());
            return false;
        }
    }

    private void closeQuietly(JMXConnector connector) {
        if (connector != null) {
            try {
                connector.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    private void detachQuietly(VirtualMachine vm) {
        if (vm != null) {
            try {
                vm.detach();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
