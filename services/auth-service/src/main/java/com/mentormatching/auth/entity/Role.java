package com.mentormatching.auth.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Danh mục role; không seed role hay chính sách cấp quyền trong migration đầu tiên.
 * Entity nội bộ Auth-service, không trả trực tiếp qua API.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {
    /**
     * Định danh role.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /**
     * Mã role.
     */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /**
     * Mô tả role.
     */
    @Column(name = "description", nullable = true, length = 255)
    private String description;
}
