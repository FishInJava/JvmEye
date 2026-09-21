package com.jvmeeye.monitor.dto;

import java.util.List;

/** 历史序列查询结果。 */
public record MetricsHistory(
        long pid,
        String displayName,
        long from,
        long to,
        long intervalMs,
        int requestedPoints,
        int returnedPoints,
        List<MetricsSnapshot> points) {
}
