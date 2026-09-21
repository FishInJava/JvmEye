package com.jvmeeye.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 指标采样任务:按配置间隔对所有已连接目标采样,写入各自的历史环形缓冲。
 *
 * <p>目标进程退出或 JMX 断开时自动清理连接,并记录日志。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsSampler {

    private final JmxConnectionManager connectionManager;

    @Scheduled(fixedDelayString = "${jvmeeye.monitor.sample-interval-ms:1000}",
            initialDelayString = "${jvmeeye.monitor.sample-interval-ms:1000}")
    public void sample() {
        List<JmxTargetSession> sessions = connectionManager.all();
        if (sessions.isEmpty()) {
            return;
        }
        for (JmxTargetSession session : sessions) {
            try {
                session.sample();
            } catch (IOException e) {
                log.info("target pid={} sampling failed, closing: {}", session.getPid(), e.getMessage());
                session.markClosed("采样失败: " + e.getMessage());
            } catch (Exception e) {
                log.warn("target pid={} sampling error: {}", session.getPid(), e.toString());
            }
        }
        List<Long> evicted = connectionManager.evictDeadSessions();
        if (!evicted.isEmpty()) {
            log.info("evicted dead targets: {}", evicted);
        }
    }
}
