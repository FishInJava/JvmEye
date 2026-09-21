package com.jvmeeye.monitor.dto;

import java.util.List;

/** JVM 运行时信息。 */
public record RuntimeInfo(
        String jvmName,
        String jvmVersion,
        String jvmVendor,
        String specVersion,
        String vmName,
        String vmVendor,
        String managementSpecVersion,
        long startTime,
        long uptimeMs,
        String uptimeText,
        List<String> inputArguments) {
}
