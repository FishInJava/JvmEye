package com.jvmeeye.controller;

import com.jvmeeye.discovery.JvmDiscoveryService;
import com.jvmeeye.discovery.dto.JvmProcessInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本机 JVM 发现。
 */
@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class DiscoveryController {

    private final JvmDiscoveryService discoveryService;

    /**
     * 列出本机可发现的 JVM。
     *
     * @param includeSelf 是否包含 JvmEye 自身(默认 false)
     */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Boolean includeSelf) {
        List<JvmProcessInfo> targets = discoveryService.list(Boolean.TRUE.equals(includeSelf));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("count", targets.size());
        body.put("targets", targets);
        return body;
    }
}
