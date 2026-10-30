package com.mentormatching.auth.config;

import org.springframework.context.annotation.Bean;
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
}
