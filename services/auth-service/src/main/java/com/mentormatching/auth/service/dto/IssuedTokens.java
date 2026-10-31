package com.mentormatching.auth.service.dto;

import java.time.Instant;

    /**
     * Cặp token đã phát hành và thời điểm hết hạn tương ứng.
     *
     * @param accessToken      access JWT
     * @param refreshToken     refresh token gốc
     * @param accessExpiresAt  thời điểm access token hết hạn
     * @param refreshExpiresAt thời điểm refresh token hết hạn
     */
public record IssuedTokens(String accessToken, String refreshToken, Instant accessExpiresAt,
                               Instant refreshExpiresAt) {
        @Override
        public String toString() {
            return "IssuedTokens[redacted]";
        }
    }
