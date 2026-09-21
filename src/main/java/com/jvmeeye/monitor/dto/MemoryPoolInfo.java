package com.jvmeeye.monitor.dto;

/** 单个内存池(Memory Pool)明细,单位:字节。 */
public record MemoryPoolInfo(
        String name,
        String type,
        long used,
        long committed,
        long max,
        long peakUsed,
        long peakMax,
        String usageThreshold) {
}
