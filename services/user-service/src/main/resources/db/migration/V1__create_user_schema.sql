-- MySQL 8.4 / InnoDB. UUIDs use BINARY(16), without UUID_TO_BIN swap flags.
-- Auth owns login email, credentials, roles and account status.
-- user_id is supplied by auth (accounts.id / JWT sub), never generated here.
-- DATETIME values represent UTC and are supplied by the application.
CREATE TABLE user_profiles (
    user_id BINARY(16) NOT NULL,
    full_name VARCHAR(255) NULL,
    -- Phone lưu các ký tự thuộc bộ ASCII, chữ số, chữ cái tiếng Anh, dấu +, -, khoảng trắng…
    -- Quy định so sánh và sắp xếp theo giá trị mã ký tự ASCII; phân biệt chữ hoa và chữ thường, A khác a
    phone VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NULL,
    user_type VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_profiles_phone UNIQUE (phone),
    CONSTRAINT ck_user_profiles_name CHECK (full_name IS NULL OR CHAR_LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT ck_user_profiles_phone CHECK (phone IS NULL OR CHAR_LENGTH(TRIM(phone)) > 0),
    CONSTRAINT ck_user_profiles_type CHECK (user_type IS NULL OR user_type IN
        ('STUDENT', 'PARENT', 'UNIVERSITY_STUDENT', 'WORKING_ADULT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE learner_profiles (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    gender VARCHAR(20) NULL,
    birth_year INT NULL,
    school_name VARCHAR(255) NULL,
    grade_id BIGINT NULL,
    learning_goal VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_learner_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_learner_profiles_user FOREIGN KEY (user_id) REFERENCES user_profiles (user_id),
    CONSTRAINT ck_learner_profiles_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_learner_profiles_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100),
    CONSTRAINT ck_learner_profiles_grade CHECK (grade_id IS NULL OR grade_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
