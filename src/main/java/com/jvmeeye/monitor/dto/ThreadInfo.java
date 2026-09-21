package com.jvmeeye.monitor.dto;

import java.util.List;
import java.util.Map;

/** 线程汇总信息(线程 Dump 见 diagnostics 包)。 */
public record ThreadInfo(
        int live,
        int daemon,
        int peak,
        long totalStarted,
        long currentThreadCpuTimeMs,
        long currentThreadUserTimeMs,
        Map<String, Integer> states,
        boolean deadlocked,
        int deadlockedCount,
        List<Long> deadlockedThreadIds) {
}
