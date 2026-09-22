package com.jvmeeye.controller.dto;

import com.jvmeeye.monitor.JmxTargetSession;

/**
 * POST /api/targets/{pid}/connect 响应。
 *
 * @param connected   是否已连接(恒为 true,失败会抛异常)
 * @param pid         目标进程号
 * @param displayName 目标显示名
 * @param connectedAt 连接建立时间(epoch 毫秒)
 */
public record ConnectResult(
        boolean connected,
        long pid,
        String displayName,
        long connectedAt) {

    public static ConnectResult of(JmxTargetSession session) {
        return new ConnectResult(true, session.getPid(), session.getDisplayName(), session.getConnectedAt());
    }
}
