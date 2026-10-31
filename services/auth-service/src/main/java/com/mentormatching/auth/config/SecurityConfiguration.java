package com.mentormatching.auth.config;

import com.mentormatching.auth.security.AccountUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
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

}
