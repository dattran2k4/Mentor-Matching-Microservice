package com.mentormatching.auth.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Cấu hình origin được phép gửi request và cookie đến Auth Service. */
@Configuration
public class CorsConfig {
    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${auth.cors.allowed-origins:http://localhost:5173}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList();
    }

    /** Cấu hình CORS cho frontend gửi request kèm credentials. */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
