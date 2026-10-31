package com.mentormatching.auth.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.mentormatching.auth.entity.Account;

/**
 * Token thuộc một family qua nhiều lần rotation; cập nhật rotation trong transaction.
 * Entity nội bộ Auth-service, không trả trực tiếp qua API.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    /**
     * Định danh refresh token.
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
     * Định danh chuỗi token rotation.
     */
    @Column(name = "family_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID familyId;

    /**
     * Thời điểm hết hạn.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "expires_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant expiresAt;

    /**
     * Thời điểm thu hồi.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "revoked_at", nullable = true, columnDefinition = "DATETIME(6)")
    private Instant revokedAt;

    /**
     * ID token thay thế.
     */
    @Column(name = "replaced_by_id", columnDefinition = "BINARY(16)")
    private UUID replacedById;

    /**
     * Phiên bản optimistic locking.
     */
    @Version
    @Column(nullable = false)
    private long version;
}
