package com.jvmeeye.controller.dto;

/**
 * DELETE /api/targets/disconnect 响应:断开全部目标。
 *
 * @param connected 断开后是否仍处于连接态(恒为 false)
 * @param closed    实际关闭的连接数
 */
public record DisconnectAllResult(
        boolean connected,
        int closed) {

    public static DisconnectAllResult of(int closed) {
        return new DisconnectAllResult(false, closed);
    }
}
