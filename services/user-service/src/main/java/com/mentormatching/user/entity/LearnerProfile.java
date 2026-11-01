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
 * Một hồ sơ học tập cho mỗi user; FK được quản lý bởi migration, không cascade.
 */
@Entity
@Table(name = "learner_profiles")
@Getter
@Setter
@NoArgsConstructor
public class LearnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private LearnerGender gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "school_name", length = 255)
    private String schoolName;

    /**
     * ID thuộc catalog
     */
    @Column(name = "grade_id")
    private Long gradeId;

    @Column(name = "learning_goal", length = 2000)
    private String learningGoal;

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

    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Long version;
}
