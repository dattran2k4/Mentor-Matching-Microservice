package com.mentormatching.auth.service;

import com.mentormatching.auth.dto.response.IssuedTokens;
import java.time.Instant;

import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.repository.AccountRoleRepository;
import com.mentormatching.auth.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đổi refresh token hợp lệ lấy cặp token mới và thu hồi token cũ.
 */
@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final TokenService tokenService;

    public RefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository, AccountRoleRepository accountRoleRepository,
                               TokenService tokenService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.tokenService = tokenService;
    }

    /**
     * Kiểm tra token và xoay vòng trong một transaction.
     *
     * @param rawToken refresh token gốc từ client
     * @return cặp token mới
     */
    @Transactional
    public IssuedTokens refresh(String rawToken) {

        // Token do service phát hành là 32 byte được mã hóa Base64URL không padding.
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidToken();
        }

        var current = refreshTokenRepository.findLockedByTokenHash(TokenService.hashRefreshToken(rawToken)).orElseThrow(RefreshTokenUseCase::invalidToken);

        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(Instant.now())
                || current.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw invalidToken();
        }

        var roles = accountRoleRepository.findByAccount_Id(current.getAccount().getId()).stream()
                .map(assignment -> assignment.getRole().getCode()).toList();

        return tokenService.rotate(current, roles);
    }

    private static ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token.");
    }
}
