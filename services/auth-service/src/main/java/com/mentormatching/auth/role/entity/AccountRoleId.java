package com.mentormatching.auth.role.entity;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Khóa ghép bảo đảm một account không được gán cùng role hai lần.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AccountRoleId implements Serializable {
    /**
     * Phiên bản serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * ID tài khoản.
     */
    @Column(name = "account_id", columnDefinition = "BINARY(16)")
    private UUID accountId;

    /**
     * ID role.
     */
    @Column(name = "role_id", columnDefinition = "BINARY(16)")
    private UUID roleId;
}
