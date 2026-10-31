package com.mentormatching.auth.config;

import com.mentormatching.auth.security.AccountUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cấu hình các thành phần bảo mật của Auth Service.
 */
@Configuration
public class SecurityConfiguration {
    /**
     * Tạo bộ mã hóa và kiểm tra mật khẩu bằng BCrypt.
     *
     * @return bộ mã hóa mật khẩu
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cấu hình xác thực email và mật khẩu qua Spring Security.
     *
     * @param userDetailsService bộ tải tài khoản
     * @param passwordEncoder bộ kiểm tra mật khẩu
     * @return bộ quản lý xác thực
     */
    @Bean
    AuthenticationManager authenticationManager(AccountUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /** Cho phép các endpoint xác thực công khai; API sử dụng token trong body, không dùng cookie. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                        "/api/v1/auth/refresh-token", "/api/v1/auth/logout"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

}
