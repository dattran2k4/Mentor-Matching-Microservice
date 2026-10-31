package com.mentormatching.auth.controller;

import java.time.Duration;
import java.time.Instant;

import com.mentormatching.auth.dto.response.ApiResponse;
import com.mentormatching.auth.dto.response.AuthTokensResponse;
import com.mentormatching.auth.service.RefreshTokenCookieService;
import com.mentormatching.auth.service.*;
import com.mentormatching.auth.dto.request.*;
import com.mentormatching.auth.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint đăng ký, đăng nhập, làm mới token và đăng xuất.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenCookieService cookieService;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase,
                          RefreshTokenCookieService cookieService) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.cookieService = cookieService;
    }

    /**
     * Đăng ký tài khoản đang chờ xác minh email.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResult>> register(@Valid @RequestBody RegisterCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<RegisterResult>builder()
                        .status(201)
                        .code("CREATED")
                        .success(true)
                        .message("Register successfully")
                        .data(registerUseCase.register(command))
                        .build());
    }

    /**
     * Đăng nhập, trả access token và đặt refresh token vào cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginCommand command) {
        return tokenResponse(loginUseCase.login(command), "Login successfully");
    }

    /**
     * Đổi refresh token trong cookie lấy cặp token mới.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> refresh(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
        return tokenResponse(refreshTokenUseCase.refresh(refreshToken), "Refresh token successfully");
    }

    /**
     * Thu hồi refresh token và xóa cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
        logoutUseCase.logout(refreshToken);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .body(ApiResponse.<Void>builder()
                        .status(200)
                        .code("SUCCESS")
                        .success(true)
                        .message("Logout successfully")
                        .build());
    }

    private ResponseEntity<ApiResponse<AuthTokensResponse>> tokenResponse(IssuedTokens tokens, String message) {
        Instant now = Instant.now();
        long accessSeconds = Math.max(0, Duration.between(now, tokens.accessExpiresAt()).getSeconds());
        long refreshSeconds = Math.max(0, Duration.between(now, tokens.refreshExpiresAt()).getSeconds());
        var data = new AuthTokensResponse(tokens.accessToken(), accessSeconds, refreshSeconds);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
                .header(HttpHeaders.SET_COOKIE, cookieService.create(tokens.refreshToken(), Duration.ofSeconds(refreshSeconds)).toString())
                .body(ApiResponse.<AuthTokensResponse>builder()
                        .status(200)
                        .code("SUCCESS")
                        .success(true)
                        .message(message)
                        .data(data)
                        .build());
    }

}
