package com.jvmeeye.monitor.dto;

/** CPU 与系统负载。processCpuLoad / systemCpuLoad 为 0.0~1.0,取不到时为 -1。 */
public record CpuInfo(
        double processCpuLoad,
        double systemCpuLoad,
        double systemLoadAverage,
        int availableProcessors,
        long processCpuTimeMs) {
}
