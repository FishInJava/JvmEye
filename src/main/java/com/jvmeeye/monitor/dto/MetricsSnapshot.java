package com.jvmeeye.monitor.dto;

import java.util.List;

/** 单次指标快照(实时查询与历史序列的元素结构一致)。 */
public record MetricsSnapshot(
        long timestamp,
        long pid,
        String displayName,
        RuntimeInfo runtime,
        MemoryInfo memory,
        GcInfo gc,
        ThreadInfo thread,
        CpuInfo cpu,
        ClassInfo classLoading,
        CompilationInfo compilation) {
}
