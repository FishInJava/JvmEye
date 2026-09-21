package com.jvmeeye.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JvmEye 全部可配置项(前缀 {@code jvmeeye})。
 */
@Data
@ConfigurationProperties(prefix = "jvmeeye")
public class JvmEyeProperties {

    /** 登录用户配置(内网工具,内存用户)。 */
    private User user = new User();

    /** 监控采集相关配置。 */
    private Monitor monitor = new Monitor();

    @Data
    public static class User {
        /** 登录用户名。 */
        private String username = "admin";

        /**
         * 登录密码。
         *
         * <p>直接写明文,启动时用 BCrypt 编码;如需哈希值,可写成 {@code {bcrypt}$2a$10$...}
         * 形式(带 {@code {bcrypt}} 前缀时直接使用)。</p>
         */
        private String password = "admin123";
    }

    @Data
    public static class Monitor {
        /** 采样间隔(毫秒)。 */
        private long sampleIntervalMs = 1000L;

        /** 每个目标保留的历史采样点数(环形缓冲容量)。 */
        private int historySize = 600;

        /** 发现列表是否排除 JvmEye 自身进程。 */
        private boolean excludeSelf = true;

        /** 是否允许同时连接多个目标(false = 单目标,连接新目标时释放旧连接)。 */
        private boolean multiTarget = false;
    }
}
