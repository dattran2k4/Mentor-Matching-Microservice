package com.mentormatching.auth.repository;

import com.mentormatching.auth.entity.Account;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu tài khoản.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Tìm tài khoản theo email đăng nhập đã chuẩn hóa.
     */
    Optional<Account> findByLoginEmail(String loginEmail);
}
