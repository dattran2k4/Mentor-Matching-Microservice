package com.mentormatching.auth.token.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.mentormatching.auth.account.entity.Account;

/**
 * Token đặt lại mật khẩu dùng một lần; version hỗ trợ ngăn ghi đè khi xử lý cạnh tranh.
 * Entity nội bộ Auth-service, không trả trực tiếp qua API.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    /**
     * Định danh token đặt lại mật khẩu.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Tài khoản sở hữu token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    /**
     * Hash của token.
     */
    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    /**
     * Thời điểm hết hạn.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "expires_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant expiresAt;

    /**
     * Thời điểm sử dụng token.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "consumed_at", nullable = true, columnDefinition = "DATETIME(6)")
    private Instant consumedAt;

    /**
     * Phiên bản optimistic locking.
     */
    @Version
    @Column(nullable = false)
    private long version;
}
