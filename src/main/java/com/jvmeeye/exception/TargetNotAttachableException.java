package com.jvmeeye.exception;

/** 目标进程存在但不可 attach / 连接失败(HTTP 409)。 */
public class TargetNotAttachableException extends RuntimeException {
    public TargetNotAttachableException(String message) {
        super(message);
    }

    public TargetNotAttachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
