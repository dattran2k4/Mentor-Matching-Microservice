package com.mentormatching.auth.account.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

/**
 * Định danh đăng nhập; id cũng là userId trong contract liên service.
 * Entity nội bộ Auth-service, không trả trực tiếp qua API.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {
    /**
     * Định danh tài khoản.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Email đăng nhập.
     */
    @Column(name = "login_email", nullable = false, length = 254)
    private String loginEmail;

    /**
     * Trạng thái tài khoản.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private AccountStatus status;

    /**
     * Thời điểm xác minh email.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "email_verified_at", nullable = true, columnDefinition = "DATETIME(6)")
    private Instant emailVerifiedAt;

    /**
     * Thời điểm tạo tài khoản.
     */
    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)", updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm cập nhật tài khoản.
     */
    @UpdateTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    /**
     * Phiên bản optimistic locking.
     */
    @Version
    @Column(nullable = false)
    private long version;
}
