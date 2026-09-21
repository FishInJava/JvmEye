package com.jvmeeye.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * 登录用户与密码编码器配置。
 *
 * <p>用户来自 {@code application.yml} 的 {@code jvmeeye.user.*},内网工具单用户即可。</p>
 */
@Configuration
@RequiredArgsConstructor
public class UserConfig {

    private final JvmEyeProperties properties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        String raw = properties.getUser().getPassword();
        String encoded = raw != null && raw.startsWith("{bcrypt}")
                ? raw.substring("{bcrypt}".length())
                : encoder.encode(raw == null ? "" : raw);
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(
                User.withUsername(properties.getUser().getUsername() == null
                                ? "admin" : properties.getUser().getUsername())
                        .password(encoded)
                        .roles("USER")
                        .build());
        return manager;
    }
}
