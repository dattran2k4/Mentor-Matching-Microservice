package com.mentormatching.auth.service;

import java.time.Instant;

import com.mentormatching.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thu hồi refresh token của phiên đăng xuất.
 */
@Service
public class LogoutUseCase {
    
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Thu hồi token nếu tồn tại và chưa bị thu hồi; gọi lại không thay đổi kết quả.
     *
     * @param rawToken refresh token gốc từ client, có thể null
     */
    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            return;
        }
        refreshTokenRepository.findLockedByTokenHash(TokenService.hashRefreshToken(rawToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }
}
