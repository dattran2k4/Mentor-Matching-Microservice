package com.mentormatching.auth.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật API ban đầu và nối các security handler trả ProblemDetail.
 * Chưa triển khai JWT, user store hoặc phân quyền nghiệp vụ.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    /**
     * Cho phép health endpoint và ERROR dispatch; các request còn lại yêu cầu xác thực.
     * Dùng HTTP Basic cùng handler 401/403 tùy chỉnh; tắt form login và request cache.
     * Giữ CSRF mặc định cho đến khi chốt cơ chế token/session.
     * Cho phép ERROR dispatch không có nghĩa là mở công khai mọi request trực tiếp tới /error.
     *
     * @param http builder cấu hình Spring Security
     * @param entryPoint handler ghi response 401
     * @param deniedHandler handler ghi response 403
     * @return security filter chain áp dụng cho service
     * @throws Exception nếu không xây dựng được cấu hình security
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint,
            ProblemAccessDeniedHandler deniedHandler) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // Keep CSRF enabled until the token/session design is decided.
                .build();
    }
}
