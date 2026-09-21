package com.jvmeeye.monitor.dto;

/** 单个垃圾收集器的累计统计。 */
public record GcCollectorInfo(String name, long collectionCount, long collectionTimeMs) {
}
