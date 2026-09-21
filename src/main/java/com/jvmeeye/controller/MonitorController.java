package com.jvmeeye.controller;

import com.jvmeeye.discovery.JvmDiscoveryService;
import com.jvmeeye.exception.TargetNotAttachableException;
import com.jvmeeye.monitor.JmxConnectionManager;
import com.jvmeeye.monitor.JmxTargetSession;
import com.jvmeeye.monitor.MetricsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 目标连接 / 断开与连接状态查询。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MonitorController {

    private final JmxConnectionManager connectionManager;
    private final JvmDiscoveryService discoveryService;
    private final MetricsQueryService metricsQueryService;

    /** 连接到指定 pid 的 JVM。 */
    @PostMapping("/targets/{pid}/connect")
    public Map<String, Object> connect(@PathVariable long pid) {
        boolean known = discoveryService.list(true).stream()
                .anyMatch(info -> info.pid() == pid);
        if (!known && !isAlive(pid)) {
            throw new TargetNotAttachableException("找不到 pid=" + pid + " 对应的本机 JVM");
        }
        JmxTargetSession session = connectionManager.connect(pid);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("connected", true);
        body.put("pid", session.getPid());
        body.put("displayName", session.getDisplayName());
        body.put("connectedAt", session.getConnectedAt());
        return body;
    }

    /** 断开指定目标;不带 pid 时断开全部。 */
    @DeleteMapping("/targets/{pid}/disconnect")
    public Map<String, Object> disconnect(@PathVariable long pid) {
        int closed = connectionManager.disconnect(pid);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("connected", closed > 0);
        body.put("pid", pid);
        body.put("closed", closed);
        return body;
    }

    /** 断开全部目标。 */
    @DeleteMapping("/targets/disconnect")
    public Map<String, Object> disconnectAll() {
        int closed = connectionManager.disconnect(null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("connected", false);
        body.put("closed", closed);
        return body;
    }

    /** 当前连接状态。 */
    @GetMapping("/monitor/status")
    public MetricsQueryService.ConnectionStatus status() {
        return metricsQueryService.status();
    }

    private boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
