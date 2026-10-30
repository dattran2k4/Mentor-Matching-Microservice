package com.mentormatching.auth.role.repository;

import com.mentormatching.auth.role.entity.AccountRole;
import com.mentormatching.auth.role.entity.AccountRoleId;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu liên kết tài khoản và role.
 */
@Repository
public interface AccountRoleRepository extends JpaRepository<AccountRole, AccountRoleId> {

    /**
     * Tìm các role được gán cho tài khoản.
     */
    List<AccountRole> findByAccount_Id(UUID accountId);
}
