package com.mentormatching.auth.role.entity;

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
 * Liên kết account–role trong cùng database, không cascade xóa account hoặc role.
 * Entity nội bộ Auth-service, không trả trực tiếp qua API.
 */
@Entity
@Table(name = "account_roles")
@Getter
@Setter
@NoArgsConstructor
public class AccountRole {
    /**
     * Khóa ghép tài khoản và role.
     */
    @EmbeddedId
    private AccountRoleId id = new AccountRoleId();

    /**
     * Tài khoản được gán role.
     */
    @MapsId("accountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    /**
     * Role được gán.
     */
    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, updatable = false)
    private Role role;

    /**
     * Thời điểm gán role.
     */
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "assigned_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant assignedAt;
}
