package com.mentormatching.auth.filter;

import java.io.IOException;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra Origin cho các POST xác thực sử dụng cookie trình duyệt.
 */
@Component

public class OriginFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;

    private static final Set<String> PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token", "/api/v1/auth/logout"
    );

    public OriginFilter(@Value("${auth.cors.allowed-origins:http://localhost:5173}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if ("POST".equals(request.getMethod()) && PATHS.contains(request.getServletPath())
                && (origin == null || !allowedOrigins.contains(origin))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
