package com.jvmeeye.controller.dto;

import com.jvmeeye.discovery.dto.JvmProcessInfo;

import java.util.List;

/**
 * GET /api/targets 响应:本机可发现 JVM 列表。
 *
 * @param timestamp 生成时间(epoch 毫秒),列表是"某一时刻"的快照
 * @param count     列表长度,等于 targets.size()
 * @param targets   目标列表
 */
public record TargetsResponse(
        long timestamp,
        int count,
        List<JvmProcessInfo> targets) {

    public static TargetsResponse of(List<JvmProcessInfo> targets) {
        return new TargetsResponse(System.currentTimeMillis(), targets.size(), targets);
    }
}
