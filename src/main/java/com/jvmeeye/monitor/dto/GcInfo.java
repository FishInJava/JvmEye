package com.jvmeeye.monitor.dto;

import java.util.List;

/** GC 汇总:各收集器累计次数/耗时 + 最近一次 GC。 */
public record GcInfo(
        List<GcCollectorInfo> collectors,
        long totalCollectionCount,
        long totalCollectionTimeMs,
        LastGcInfo lastGc) {
}
