-- Auth owns all tables below. MySQL 8.4 / InnoDB; UUID = BINARY(16).
-- Emails are normalized by the application; accent-sensitive, case-insensitive uniqueness.
-- All DATETIME values represent UTC. Tokens store SHA-256 lowercase hex, never raw secrets.
CREATE TABLE accounts (
    id BINARY(16) NOT NULL,
    login_email VARCHAR(254) COLLATE utf8mb4_0900_as_ci NOT NULL,
    status VARCHAR(32) NOT NULL,
    email_verified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_login_email UNIQUE (login_email),
    CONSTRAINT ck_accounts_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE password_credentials (
    account_id BINARY(16) NOT NULL,
    password_hash VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    password_changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_id),
    CONSTRAINT fk_password_credentials_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    code VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE account_roles (
    account_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_id, role_id),
    KEY idx_account_roles_role (role_id),
    CONSTRAINT fk_account_roles_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_account_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE refresh_tokens (
    id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    family_id BINARY(16) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    replaced_by_id BINARY(16) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT uk_refresh_tokens_replacement UNIQUE (replaced_by_id),
    CONSTRAINT uk_refresh_tokens_identity_family UNIQUE (id, account_id, family_id),
    KEY idx_refresh_tokens_account (account_id),
    KEY idx_refresh_tokens_family (family_id),
    KEY idx_refresh_tokens_expiry (expires_at),
    CONSTRAINT fk_refresh_tokens_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_id, account_id, family_id)
        REFERENCES refresh_tokens (id, account_id, family_id),
    CONSTRAINT ck_refresh_tokens_not_self CHECK (replaced_by_id IS NULL OR replaced_by_id <> id),
    CONSTRAINT ck_refresh_tokens_rotation CHECK (replaced_by_id IS NULL OR revoked_at IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE email_verification_tokens (
    id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    email_to_verify VARCHAR(254) COLLATE utf8mb4_0900_as_ci NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash),
    KEY idx_email_verification_tokens_account (account_id),
    KEY idx_email_verification_tokens_expiry (expires_at),
    CONSTRAINT fk_email_verification_tokens_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE password_reset_tokens (
    id BINARY(16) NOT NULL,
    account_id BINARY(16) NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    KEY idx_password_reset_tokens_account (account_id),
    KEY idx_password_reset_tokens_expiry (expires_at),
    CONSTRAINT fk_password_reset_tokens_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
