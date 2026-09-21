package com.jvmeeye.monitor;

import com.jvmeeye.config.JvmEyeProperties;
import com.jvmeeye.exception.ApiException;
import com.jvmeeye.monitor.dto.MetricsHistory;
import com.jvmeeye.monitor.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 指标查询:实时当前值 + 历史序列。
 */
@Service
@RequiredArgsConstructor
public class MetricsQueryService {

    private final JmxConnectionManager connectionManager;
    private final JvmEyeProperties properties;

    /** 实时采集一次(不入缓冲)。 */
    public MetricsSnapshot current() {
        JmxTargetSession session = connectionManager.active();
        try {
            return session.collect();
        } catch (IOException e) {
            session.markClosed("查询失败: " + e.getMessage());
            throw new ApiException(HttpStatus.CONFLICT.value(),
                    "目标 JVM 连接已断开(pid=" + session.getPid() + "): " + e.getMessage(), e);
        }
    }

    /**
     * 取最近 N 点历史。
     *
     * @param points 需要的点数,默认 300,上限为缓冲容量
     */
    public MetricsHistory history(Integer points) {
        JmxTargetSession session = connectionManager.active();
        int capacity = properties.getMonitor().getHistorySize();
        int requested = points == null ? Math.min(300, capacity) : points;
        if (requested < 1) {
            throw new IllegalArgumentException("points 必须 >= 1");
        }
        requested = Math.min(requested, capacity);
        List<MetricsSnapshot> result = session.history(requested);
        long from = result.isEmpty() ? 0 : result.get(0).timestamp();
        long to = result.isEmpty() ? 0 : result.get(result.size() - 1).timestamp();
        return new MetricsHistory(session.getPid(), session.getDisplayName(), from, to,
                properties.getMonitor().getSampleIntervalMs(), requested, result.size(), result);
    }

    /** 连接状态信息。 */
    public ConnectionStatus status() {
        JmxTargetSession session = connectionManager.activeOrNull();
        if (session == null) {
            return new ConnectionStatus(false, null, null, 0, null, null);
        }
        return new ConnectionStatus(true, session.getPid(), session.getDisplayName(),
                session.historySize(), session.getConnectedAt(), session.getCloseReason());
    }

    /** 连接状态 DTO。 */
    public record ConnectionStatus(boolean connected, Long pid, String displayName,
                                   int bufferedPoints, Long connectedAt, String closeReason) {
    }
}
