package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.RefreshToken;

import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu refresh token.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Tìm token theo hash, không tra cứu bằng token gốc.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Khóa token trong transaction để ngăn hai lần rotation đồng thời. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findLockedByTokenHash(String tokenHash);
}
