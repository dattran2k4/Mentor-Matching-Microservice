package com.mentormatching.auth.service;

import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Tạo và xóa cookie chứa refresh token.
 */
@Component
public class RefreshTokenCookieService {
    
    public static final String COOKIE_NAME = "refresh_token";
    private final String path;
    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieService(@Value("${auth.cookie.path:/api/v1/auth}") String path,
                                     @Value("${auth.cookie.secure:false}") boolean secure,
                                     @Value("${auth.cookie.same-site:Strict}") String sameSite) {
        if (!Set.of("Strict", "Lax", "None").contains(sameSite)
                || (sameSite.equals("None") && !secure) || path.isBlank()) {
            throw new IllegalArgumentException("Invalid refresh cookie configuration");
        }
        this.path = path;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /**
     * Tạo cookie chứa token với thời hạn được cung cấp.
     */
    public ResponseCookie create(String token, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    /**
     * Tạo cookie hết hạn với cùng path để trình duyệt xóa cookie cũ.
     */
    public ResponseCookie clear() {
        return create("", Duration.ZERO);
    }
}