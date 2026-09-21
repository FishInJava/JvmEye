package com.jvmeeye.monitor.dto;

/** 最近一次 GC 信息(通过 {@code LastGcInfo} 内存池属性推导)。 */
public record LastGcInfo(String name, long id, long startTime, long endTime, long durationMs) {
}
