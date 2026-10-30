package com.mentormatching.auth.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Cấu hình khóa RSA và bộ ký access JWT.
 */
@Configuration
public class TokenConfiguration {

    /**
     * Đọc khóa RSA riêng định dạng PKCS#8 và tạo JWK chứa cặp khóa để ký JWT.
     * Khởi tạo khi cần để ứng dụng có thể chạy trước khi cấu hình chức năng phát hành token.
     *
     * @param privateKeyPath đường dẫn đến file private key định dạng PEM
     * @return JWK chứa khóa RSA
     * @throws Exception khi khóa không hợp lệ hoặc không thể tạo khóa công khai
     */
    @Bean
    @Lazy
    RSAKey jwtSigningKey(@Value("${auth.jwt.private-key-path}") String privateKeyPath) throws Exception {

        String pemKey = Files.readString(Path.of(privateKeyPath));
        String base64 = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Chuyển chuỗi Base64 thành các byte gốc.
        byte[] der = Base64.getDecoder().decode(base64);

        //Tạo đối tượng khóa RSA.
        KeyFactory factory = KeyFactory.getInstance("RSA");

        //Byte DER bên trên chứa private key
        //Được mã hóa theo định dạng PKCS#8
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(der);

        // Tạo đối tượng private key
        RSAPrivateCrtKey privateRsaKey = (RSAPrivateCrtKey) factory.generatePrivate(keySpec);

        if (privateRsaKey.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("RSA key must be at least 2048 bits");
        }

        // Public key RSA gồm hai thành phần chính: modulus (n) và public exponent (e)
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                privateRsaKey.getModulus(),
                privateRsaKey.getPublicExponent()
        );

        // Tạo đối tượng public key từ n và e.
        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(publicKeySpec);

        //Gói public key và private key vào một đối tượng
        return new RSAKey.Builder(publicKey)
                .privateKey(privateRsaKey)
                .build();
    }

    /**
     * Tạo bộ mã hóa JWT sử dụng khóa RSA; chữ ký được tạo khi gọi encode.
     * Lazy trì hoãn việc đọc và kiểm tra khóa đến khi cần phát hành token;
     * cấu hình thiếu hoặc sai sẽ gây lỗi tại thời điểm đó thay vì khi khởi động.
     *
     * @param jwtSigningKey JWK chứa khóa ký
     * @return bộ mã hóa và ký JWT
     */
    @Bean
    @Lazy
    JwtEncoder jwtEncoder(RSAKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtSigningKey)));
    }
}
