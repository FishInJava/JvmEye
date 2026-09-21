package com.jvmeeye;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spring 上下文加载 + 关键 Bean 装配检查。 */
@SpringBootTest
class JvmEyeApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    com.jvmeeye.config.JvmEyeProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    com.jvmeeye.discovery.JvmDiscoveryService discoveryService;

    @org.springframework.beans.factory.annotation.Autowired
    com.jvmeeye.monitor.JmxConnectionManager connectionManager;

    @org.springframework.beans.factory.annotation.Autowired
    com.jvmeeye.diagnostics.ThreadDumpService threadDumpService;

    @org.springframework.beans.factory.annotation.Autowired
    com.jvmeeye.diagnostics.HeapHistogramService heapHistogramService;

    @Test
    void contextLoads() {
        assertTrue(properties.getMonitor().getHistorySize() > 0, "历史缓冲容量应大于 0");
        assertTrue(properties.getMonitor().getSampleIntervalMs() > 0, "采样间隔应大于 0");
    }

    @Test
    void beansAreUsable() {
        assertTrue(discoveryService != null);
        assertTrue(connectionManager != null);
        assertTrue(threadDumpService != null);
        assertTrue(heapHistogramService != null);
        assertTrue(connectionManager.all().isEmpty(), "启动后不应有活动连接");
    }
}
