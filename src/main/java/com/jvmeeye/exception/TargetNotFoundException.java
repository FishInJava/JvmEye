package com.jvmeeye.exception;

/** 请求的目标进程不存在或不可发现(HTTP 404)。 */
public class TargetNotFoundException extends RuntimeException {
    public TargetNotFoundException(String message) {
        super(message);
    }
}
