package com.jvmeeye.discovery;

import com.jvmeeye.config.JvmEyeProperties;
import com.jvmeeye.discovery.dto.JvmProcessInfo;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * 本机 JVM 发现服务。
 *
 * <p>基于 JDK Attach API 的 {@link VirtualMachine#list()} 枚举本机 HotSpot JVM;
 * 对每个候选进程执行一次 attach,读取 {@code sun.java.command}、{@code java.version}
 * 等系统属性,并判断可 attach 性(同用户、同架构、权限足够)。</p>
 *
 * <p>注意:attach 只是短连接读取属性,不会加载 agent;只有选中目标并连接时才会
 * 调用 {@code startLocalManagementAgent()} 开启 JMX 本地连接。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JvmDiscoveryService {

    private final JvmEyeProperties properties;

    /**
     * 列出本机 JVM。
     *
     * @param includeSelf 是否包含 JvmEye 自身进程
     * @return 按 pid 升序排列的列表
     */
    public List<JvmProcessInfo> list(boolean includeSelf) {
        long selfPid = ProcessHandle.current().pid();
        List<JvmProcessInfo> result = new ArrayList<>();
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            long pid;
            try {
                pid = Long.parseLong(descriptor.id());
            } catch (NumberFormatException e) {
                continue;
            }
            boolean self = pid == selfPid;
            if (self && !includeSelf && properties.getMonitor().isExcludeSelf()) {
                continue;
            }
            result.add(describe(descriptor, pid, self));
        }
        result.sort(Comparator.comparingLong(JvmProcessInfo::pid));
        return result;
    }

    /** 使用全局配置(默认排除自身)。 */
    public List<JvmProcessInfo> list() {
        return list(false);
    }

    private JvmProcessInfo describe(VirtualMachineDescriptor descriptor, long pid, boolean self) {
        String mainClass = null;
        String mainArgs = null;
        String jvmVersion = null;
        String jvmName = null;
        String javaHome = null;
        boolean attachable = true;
        String reason = null;

        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(descriptor.id());
            Properties systemProperties = vm.getSystemProperties();
            mainClass = firstToken(systemProperties.getProperty("sun.java.command"));
            mainArgs = restTokens(systemProperties.getProperty("sun.java.command"));
            jvmVersion = systemProperties.getProperty("java.version");
            jvmName = systemProperties.getProperty("java.vm.name");
            javaHome = systemProperties.getProperty("java.home");
        } catch (Throwable t) {
            attachable = false;
            reason = describeAttachFailure(t);
            log.debug("attach pid={} failed: {}", pid, reason);
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }

        String displayName = descriptor.displayName();
        if (mainClass == null || mainClass.isBlank()) {
            mainClass = self ? "com.jvmeeye.JvmEyeApplication" : displayName;
        }
        return new JvmProcessInfo(
                pid,
                displayName,
                mainClass,
                mainArgs,
                jvmVersion,
                jvmName,
                javaHome,
                processUser(pid),
                attachable,
                self,
                reason);
    }

    private String processUser(long pid) {
        try {
            Path status = Path.of("/proc", Long.toString(pid), "status");
            if (Files.exists(status)) {
                for (String line : Files.readAllLines(status)) {
                    if (line.startsWith("Uid:")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 1) {
                            return parts[1];
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // best effort
        }
        return null;
    }

    private String describeAttachFailure(Throwable t) {
        String message = t.getMessage();
        if (t instanceof IOException && message != null && message.contains("current VM")) {
            return "不能 attach 到 JvmEye 自身进程";
        }
        if (t instanceof com.sun.tools.attach.AttachNotSupportedException) {
            return "目标不是可 attach 的 HotSpot JVM(可能已退出或架构不同)";
        }
        if (t instanceof IOException && message != null && message.toLowerCase().contains("permission")) {
            return "权限不足:目标进程属于其他用户,需要同用户或 root 运行";
        }
        return message != null ? message : t.getClass().getSimpleName();
    }

    private String firstToken(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }
        String trimmed = command.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private String restTokens(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }
        String trimmed = command.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? null : trimmed.substring(space + 1);
    }
}
