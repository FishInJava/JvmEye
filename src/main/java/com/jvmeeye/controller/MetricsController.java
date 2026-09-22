package com.jvmeeye.controller;

import com.jvmeeye.monitor.MetricsQueryService;
import com.jvmeeye.monitor.dto.MetricsHistory;
import com.jvmeeye.monitor.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指标查询:实时当前值 + 历史序列。
 *
 * <p>所有响应体都是 {@code com.jvmeeye.monitor.dto} 下的 record,统一用 {@link ResponseEntity} 包装;
 * 失败时由 {@link com.jvmeeye.exception.GlobalExceptionHandler} 转换为 ProblemDetail(RFC 7807)。</p>
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsQueryService metricsQueryService;

    /** 实时当前指标快照。 */
    @GetMapping("/current")
    public ResponseEntity<MetricsSnapshot> current() {
        return ResponseEntity.ok(metricsQueryService.current());
    }

    /**
     * 最近 N 点历史(按时间升序),默认 300 点。
     *
     * @param points 需要的点数(可选)
     */
    @GetMapping("/history")
    public ResponseEntity<MetricsHistory> history(@RequestParam(required = false) Integer points) {
        return ResponseEntity.ok(metricsQueryService.history(points));
    }
}
