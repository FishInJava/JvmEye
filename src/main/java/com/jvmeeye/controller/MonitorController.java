package com.jvmeeye.controller;

import com.jvmeeye.controller.dto.ConnectResult;
import com.jvmeeye.controller.dto.DisconnectAllResult;
import com.jvmeeye.controller.dto.DisconnectResult;
import com.jvmeeye.discovery.JvmDiscoveryService;
import com.jvmeeye.exception.TargetNotAttachableException;
import com.jvmeeye.monitor.JmxConnectionManager;
import com.jvmeeye.monitor.JmxTargetSession;
import com.jvmeeye.monitor.MetricsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目标连接 / 断开与连接状态查询。
 *
 * <p>所有响应体都是 {@code com.jvmeeye.controller.dto} 下的 record,不使用 Map 拼 JSON。
 * 失败时由 {@link com.jvmeeye.exception.GlobalExceptionHandler} 统一转换为 ProblemDetail(RFC 7807)。</p>
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
    public ResponseEntity<ConnectResult> connect(@PathVariable long pid) {
        boolean known = discoveryService.list(true).stream()
                .anyMatch(info -> info.pid() == pid);
        if (!known && !isAlive(pid)) {
            throw new TargetNotAttachableException("找不到 pid=" + pid + " 对应的本机 JVM");
        }
        return ResponseEntity.ok(ConnectResult.of(connectionManager.connect(pid)));
    }

    /** 断开指定目标;不带 pid 时断开全部。 */
    @DeleteMapping("/targets/{pid}/disconnect")
    public ResponseEntity<DisconnectResult> disconnect(@PathVariable long pid) {
        return ResponseEntity.ok(DisconnectResult.of(pid, connectionManager.disconnect(pid)));
    }

    /** 断开全部目标。 */
    @DeleteMapping("/targets/disconnect")
    public ResponseEntity<DisconnectAllResult> disconnectAll() {
        return ResponseEntity.ok(DisconnectAllResult.of(connectionManager.disconnect(null)));
    }

    /** 当前连接状态。 */
    @GetMapping("/monitor/status")
    public ResponseEntity<MetricsQueryService.ConnectionStatus> status() {
        return ResponseEntity.ok(metricsQueryService.status());
    }

    /** pid 是否对应一个活着的进程(发现列表里没有时用它兜底判活)。 */
    private boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
