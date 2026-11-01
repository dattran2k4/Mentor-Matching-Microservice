package com.mentormatching.user.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Hồ sơ người dùng. Không chứa credentials, role hoặc trạng thái account.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    /**
     * UUID do auth-service cấp; không dùng @GeneratedValue.
     */
    @Id
    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "full_name", length = 255)
    private String fullName;

    /**
     * Caller chuẩn hóa phone trước khi lưu; NULL biểu diễn chưa cung cấp.
     */
    @Column(length = 20, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_type", length = 50)
    private UserType userType;

    @CreationTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    /**
     * Nullable wrapper giúp Spring Data nhận biết entity mới có ID được cấp sẵn.
     */
    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Long version;
}
