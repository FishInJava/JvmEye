package com.jvmeeye.monitor.dto;

import java.util.List;

/** 堆 / 非堆 / 内存池明细(单位:字节)。 */
public record MemoryInfo(
        long heapUsed,
        long heapCommitted,
        long heapMax,
        long heapInit,
        double heapUsedPercent,
        long nonHeapUsed,
        long nonHeapCommitted,
        long nonHeapMax,
        long objectPendingFinalizationCount,
        List<MemoryPoolInfo> pools) {
}
