package com.jvmeeye.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.io.IOException;

/**
 * Spring Security 规则 + 单页应用(SPA)静态资源回退。
 *
 * <ul>
 *   <li>{@code /api/**} 必须登录;</li>
 *   <li>{@code /login}、静态资源、首页放行;</li>
 *   <li>CSRF 关闭(内网工具,前端用 cookie 会话);</li>
 *   <li>未登录访问 SPA 深链(如 /dashboard)时重定向到登录页;</li>
 *   <li>登录后访问未知且不带扩展名的路径统一回退到 {@code index.html},支持 history 路由刷新不 404。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/logout", "/error").permitAll()
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/vite.svg").permitAll()
                        .requestMatchers("/assets/**", "/css/**", "/js/**", "/img/**", "/fonts/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());
        return http.build();
    }

    /**
     * 让 {@code classpath:/static/} 下不存在的、无扩展名路径回退到 index.html(SPA history 模式)。
     */
    @Configuration
    public static class SpaWebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .resourceChain(true)
                    .addResolver(new SpaIndexResourceResolver());
        }
    }

    /**
     * 优先返回真实静态资源;找不到时回退 index.html(仅对非 API、非静态扩展名路径生效)。
     */
    static class SpaIndexResourceResolver extends PathResourceResolver {

        @Override
        public Resource resolveResource(jakarta.servlet.http.HttpServletRequest request,
                                        String requestPath,
                                        java.util.List<? extends Resource> locations,
                                        ResourceResolverChain chain) {
            Resource resolved = super.resolveResource(request, requestPath, locations, chain);
            if (resolved != null) {
                return resolved;
            }
            if (requestPath.startsWith("api/") || hasFileExtension(requestPath)) {
                return null;
            }
            try {
                Resource index = new org.springframework.core.io.ClassPathResource("/static/index.html");
                return index.exists() ? index : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            return null;
        }

        private boolean hasFileExtension(String path) {
            int slash = path.lastIndexOf('/');
            String last = slash >= 0 ? path.substring(slash + 1) : path;
            return last.contains(".");
        }
    }
}
