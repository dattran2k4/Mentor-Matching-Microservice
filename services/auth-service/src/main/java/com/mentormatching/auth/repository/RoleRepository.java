package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.Role;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu role.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Tìm role theo mã.
     */
    Optional<Role> findByCode(String code);
}
