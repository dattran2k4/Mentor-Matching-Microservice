package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.PasswordResetToken;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu token đặt lại mật khẩu.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Tìm token theo hash, không tra cứu bằng token gốc.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
