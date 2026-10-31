package com.mentormatching.auth.controller;

import com.mentormatching.auth.service.LoginUseCase;
import com.mentormatching.auth.service.LogoutUseCase;
import com.mentormatching.auth.service.RefreshTokenUseCase;
import com.mentormatching.auth.service.RegisterUseCase;
import com.mentormatching.auth.service.dto.IssuedTokens;
import com.mentormatching.auth.service.dto.LoginCommand;
import com.mentormatching.auth.service.dto.RefreshTokenRequest;
import com.mentormatching.auth.service.dto.RegisterCommand;
import com.mentormatching.auth.service.dto.RegisterResult;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    /**
     * Đăng ký tài khoản đang chờ xác minh email.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResult> register(@Valid @RequestBody RegisterCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.register(command));
    }

    /**
     * Đăng nhập và trả về cặp token.
     */
    @PostMapping("/login")
    public ResponseEntity<IssuedTokens> login(@Valid @RequestBody LoginCommand command) {
        return tokenResponse(loginUseCase.login(command));
    }

    /**
     * Đổi refresh token lấy cặp token mới.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<IssuedTokens> refresh(@RequestBody RefreshTokenRequest request) {
        return tokenResponse(refreshTokenUseCase.refresh(request.refreshToken()));
    }

    /**
     * Thu hồi refresh token của phiên hiện tại.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        logoutUseCase.logout(request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<IssuedTokens> tokenResponse(IssuedTokens tokens) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(tokens);
    }
}
