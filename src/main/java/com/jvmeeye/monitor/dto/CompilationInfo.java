package com.jvmeeye.monitor.dto;

/** JIT 编译统计(无编译器的 JVM 下为 null)。 */
public record CompilationInfo(String name, long totalCompilationTimeMs) {
}
