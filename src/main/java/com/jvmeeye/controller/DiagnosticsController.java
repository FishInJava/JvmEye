package com.jvmeeye.controller;

import com.jvmeeye.diagnostics.HeapHistogramService;
import com.jvmeeye.diagnostics.ThreadDumpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 诊断能力:线程 Dump 与类直方图。
 *
 * <p>所有响应体都是诊断服务返回的 record({@link ThreadDumpService.ThreadDump} /
 * {@link HeapHistogramService.Histogram}),统一用 {@link ResponseEntity} 包装;
 * 失败时由 {@link com.jvmeeye.exception.GlobalExceptionHandler} 转换为 ProblemDetail(RFC 7807)。</p>
 */
@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final ThreadDumpService threadDumpService;
    private final HeapHistogramService heapHistogramService;

    /** 完整线程 Dump(带堆栈、锁、死锁标记)。 */
    @GetMapping("/thread-dump")
    public ResponseEntity<ThreadDumpService.ThreadDump> threadDump() {
        return ResponseEntity.ok(threadDumpService.dump());
    }

    /**
     * Top N 类直方图(按字节数降序)。
     *
     * <p>注意:该操作会触发目标 JVM 一次 Full GC。</p>
     *
     * @param top 返回条数,默认 30,最大 500
     */
    @GetMapping("/histogram")
    public ResponseEntity<HeapHistogramService.Histogram> histogram(@RequestParam(required = false) Integer top) {
        return ResponseEntity.ok(heapHistogramService.histogram(top));
    }
}
