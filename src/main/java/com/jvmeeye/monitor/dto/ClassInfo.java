package com.jvmeeye.monitor.dto;

/** 类加载统计。 */
public record ClassInfo(int loaded, long totalLoaded, long unloaded, boolean verbose) {
}
