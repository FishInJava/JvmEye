package com.jvmeeye.controller.dto;

/**
 * DELETE /api/targets/{pid}/disconnect 响应。
 *
 * @param connected 断开后是否仍处于连接态(恒为 false)
 * @param pid       被断开的进程号
 * @param closed    实际关闭的连接数(0 表示该 pid 本来就没有连接)
 */
public record DisconnectResult(
        boolean connected,
        long pid,
        int closed) {

    public static DisconnectResult of(long pid, int closed) {
        return new DisconnectResult(false, pid, closed);
    }
}
