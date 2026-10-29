package com.mentormatching.auth.account.repository;

import com.mentormatching.auth.account.entity.PasswordCredential;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Truy cập dữ liệu thông tin mật khẩu.
 */
@Repository
public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, UUID> {
}
