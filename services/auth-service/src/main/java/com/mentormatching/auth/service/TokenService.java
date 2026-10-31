package com.mentormatching.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.mentormatching.auth.account.entity.Account;
import com.mentormatching.auth.token.entity.RefreshToken;
import com.mentormatching.auth.token.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phát hành access token và refresh token cho tài khoản đã xác thực.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String issuer;
    private final String audience;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    /**
     * Khởi tạo service và kiểm tra cấu hình token.
     *
     * @param encoder                bộ ký JWT
     * @param refreshTokenRepository repository lưu refresh token
     * @param issuer                 định danh bên phát hành
     * @param audience               đối tượng nhận token
     * @param accessTtl              thời hạn access token
     * @param refreshTtl             thời hạn refresh token
     */
    public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokenRepository,
                        @Value("${auth.jwt.issuer}") String issuer, @Value("${auth.jwt.audience}") String audience,
                        @Value("${auth.jwt.access-ttl}") Duration accessTtl, @Value("${auth.jwt.refresh-ttl}") Duration refreshTtl) {

        //validate
        if (issuer.isBlank() || audience.isBlank() || accessTtl.isZero() || accessTtl.isNegative()
                || refreshTtl.isZero() || refreshTtl.isNegative()) {
            throw new IllegalArgumentException("Token configuration must be non-empty and TTLs positive");
        }

        this.encoder = encoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    /**
     * Phát hành cặp token và lưu hash của refresh token.
     * Caller phải xác thực và kiểm tra tài khoản trước khi gọi.
     *
     * @param account tài khoản đã xác thực
     * @param roles   danh sách role của tài khoản
     * @return cặp token và thời điểm hết hạn
     */
    @Transactional
    public IssuedTokens issue(Account account, List<String> roles) {

        Instant now = Instant.now();

        // Payload (claims) của access token JWT.
        var claims = JwtClaimsSet.builder()

                // ai phát hành token - "auth-service".
                .issuer(issuer)

                // token được phát hành để sử dụng ở đâu
                .audience(List.of(audience))

                // định danh của người dùng
                // account ID để các service nhận biết cùng một user
                .subject(account.getId().toString())

                // thời điểm phát hành.
                .issuedAt(now)
                .expiresAt(now.plus(accessTtl))
                .claim("roles", List.copyOf(roles)).build();

        // Header chỉ định thuật toán ký JWT là RS256.
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        // Encoder lấy private key để ký header + claims
        String accessToken;
        try {
            accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        } catch (JwtEncodingException e) {
            log.error("Không thể ký access token cho accountId={}", account.getId());
            throw e;
        }

        // Tạo 32 byte ngẫu nhiên bằng SecureRandom.
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        // Biểu diễn 32 byte thành chuỗi Base64URL không có dấu padding "=".
        // Thuận tiện để truyền trong HTTP/cookie.
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refresh = new RefreshToken();
        refresh.setAccount(account);
        refresh.setFamilyId(UUID.randomUUID());
        refresh.setTokenHash(hashRefreshToken(rawRefreshToken));
        refresh.setExpiresAt(now.plus(refreshTtl));

        //Save Refresh Token
        refreshTokenRepository.save(refresh);
        log.debug("Đã chuẩn bị cặp token cho accountId={}, đang chờ transaction hoàn tất", account.getId());

        return new IssuedTokens(accessToken, rawRefreshToken, now.plus(accessTtl), now.plus(refreshTtl));
    }

    /**
     * Tính hash SHA-256 của refresh token để lưu hoặc tra cứu.
     *
     * @param raw refresh token gốc
     * @return hash dạng chuỗi hex chữ thường
     */
    public static String hashRefreshToken(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

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
}
