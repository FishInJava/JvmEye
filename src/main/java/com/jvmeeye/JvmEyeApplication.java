package com.jvmeeye;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * JvmEye 应用入口。
 *
 * <p>一个轻量级本机 JVM 监控工具:通过 JDK Attach API 发现本机 JVM,再通过
 * localConnectorAddress 建立 JMX 连接采集指标。</p>
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class JvmEyeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JvmEyeApplication.class, args);
    }
}
