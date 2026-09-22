package com.jvmeeye.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;

/**
 * 统一异常处理:所有 /api/** 错误返回一致的 ProblemDetail(RFC 7807)结构。
 *
 * <p>响应字段:{@code type / title / status / detail / instance / timestamp}。
 * 其中 {@code title / status / detail / instance} 由 Spring 的 {@link ProblemDetail} 提供,
 * 这里只额外补一个 {@code timestamp} 便于和前端日志对齐时间。</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TargetNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(TargetNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({NoActiveTargetException.class, TargetNotAttachableException.class})
    public ResponseEntity<ProblemDetail> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest request) {
        return problem(HttpStatus.valueOf(ex.getStatus()), ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "资源不存在: " + ex.getResourcePath(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleOther(Exception ex, HttpServletRequest request) {
        log.error("unhandled exception on {} {}", request.getRequestURI(), ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误: " + ex.getMessage(), request);
    }

    /** 组装 RFC 7807 响应体;detail 即原 message 字段。 */
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toEpochMilli());
        return ResponseEntity.status(status).body(problem);
    }
}
