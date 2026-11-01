package com.mentormatching.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.mentormatching.user.entity.LearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy cập và quản lý dữ liệu hồ sơ học tập.
 */
public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, UUID> {

    /**
     * Tìm hồ sơ học tập theo UUID tài khoản.
     */
    Optional<LearnerProfile> findByUserId(UUID userId);
}
