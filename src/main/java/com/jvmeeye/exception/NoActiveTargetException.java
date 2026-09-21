package com.jvmeeye.exception;

/** 当前没有已连接的监控目标(HTTP 409)。 */
public class NoActiveTargetException extends RuntimeException {
    public NoActiveTargetException(String message) {
        super(message);
    }
}
