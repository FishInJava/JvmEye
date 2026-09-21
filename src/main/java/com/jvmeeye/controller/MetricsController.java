package com.jvmeeye.controller;

import com.jvmeeye.monitor.MetricsQueryService;
import com.jvmeeye.monitor.dto.MetricsHistory;
import com.jvmeeye.monitor.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指标查询:实时当前值 + 历史序列。
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsQueryService metricsQueryService;

    /** 实时当前指标快照。 */
    @GetMapping("/current")
    public MetricsSnapshot current() {
        return metricsQueryService.current();
    }

    /**
     * 最近 N 点历史(按时间升序),默认 300 点。
     *
     * @param points 需要的点数(可选)
     */
    @GetMapping("/history")
    public MetricsHistory history(@RequestParam(required = false) Integer points) {
        return metricsQueryService.history(points);
    }
}
