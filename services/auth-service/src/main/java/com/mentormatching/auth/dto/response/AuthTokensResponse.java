package com.mentormatching.auth.dto.response;

/**
 * Access token và thời hạn; refresh token chỉ có trong cookie.
 *
 * @param accessToken access JWT
 * @param accessTokenExpiresIn thời gian còn lại của access token tính bằng giây
 * @param refreshTokenExpiresIn thời gian còn lại của refresh token tính bằng giây
 */
public record AuthTokensResponse(String accessToken, long accessTokenExpiresIn, long refreshTokenExpiresIn) {
    @Override
    public String toString() {
        return "AuthTokensResponse[redacted]";
    }
}
