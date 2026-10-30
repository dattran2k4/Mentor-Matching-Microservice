package com.mentormatching.auth.account.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mật khẩu đã hash; account có thể chưa có credential khi dùng đăng nhập ngoài.
 * Entity nội bộ Auth-service, không trả trực tiếp qua API.
 */
@Entity
@Table(name = "password_credentials")
@Getter
@Setter
@NoArgsConstructor
public class PasswordCredential {
    /**
     * ID tài khoản và khóa chính của credential.
     */
    @Id
    @Column(name = "account_id", columnDefinition = "BINARY(16)")
    private UUID accountId;

    /**
     * Tài khoản sở hữu credential; dùng chung khóa chính qua {@code @MapsId}.
     */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    /**
     * Mật khẩu đã được hash.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Thời điểm thay đổi mật khẩu.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "password_changed_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant passwordChangedAt;
}
