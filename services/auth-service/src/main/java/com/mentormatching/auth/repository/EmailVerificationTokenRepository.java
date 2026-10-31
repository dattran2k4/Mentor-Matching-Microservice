package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.EmailVerificationToken;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu token xác minh email.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Tìm token theo hash, không tra cứu bằng token gốc.
     */
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
