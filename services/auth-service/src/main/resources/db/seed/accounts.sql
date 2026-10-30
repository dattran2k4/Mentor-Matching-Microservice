-- Optional development seed for MySQL 8.4. Run after Flyway V1; not loaded automatically.
-- Accounts have no credentials and cannot be used for password login.
INSERT INTO accounts (id, login_email, status, email_verified_at, created_at, updated_at, version)
SELECT UUID_TO_BIN(UUID()), seed.email, 'PENDING_VERIFICATION', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM (
    SELECT '0901dattran@gmail.com' AS email
    UNION ALL SELECT 'letranbaophucxhb@gmail.com'
    UNION ALL SELECT 'lengocminhkien06@gmail.com'
    UNION ALL SELECT 'tranquocthanhdat@gmail.com'
    UNION ALL SELECT 'dattqde181019@fpt.edu.vn'
    UNION ALL SELECT 'account01@example.com'
    UNION ALL SELECT 'account02@example.com'
    UNION ALL SELECT 'account03@example.com'
    UNION ALL SELECT 'account04@example.com'
    UNION ALL SELECT 'account05@example.com'
    UNION ALL SELECT 'account06@example.com'
    UNION ALL SELECT 'account07@example.com'
    UNION ALL SELECT 'account08@example.com'
    UNION ALL SELECT 'account09@example.com'
    UNION ALL SELECT 'account10@example.com'
    UNION ALL SELECT 'account11@example.com'
    UNION ALL SELECT 'account12@example.com'
    UNION ALL SELECT 'account13@example.com'
    UNION ALL SELECT 'account14@example.com'
    UNION ALL SELECT 'account15@example.com'
    UNION ALL SELECT 'account16@example.com'
    UNION ALL SELECT 'account17@example.com'
    UNION ALL SELECT 'account18@example.com'
    UNION ALL SELECT 'account19@example.com'
    UNION ALL SELECT 'account20@example.com'
    UNION ALL SELECT 'account21@example.com'
    UNION ALL SELECT 'account22@example.com'
    UNION ALL SELECT 'account23@example.com'
    UNION ALL SELECT 'account24@example.com'
    UNION ALL SELECT 'account25@example.com'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE login_email = seed.email);

-- Example roles for development; existing role definitions are preserved.
INSERT INTO roles (id, code, description)
SELECT UUID_TO_BIN(UUID()), seed.code, seed.description
FROM (
    SELECT 'MENTOR' AS code, 'Mentor' AS description
    UNION ALL SELECT 'LEANER', 'Leaner'
    UNION ALL SELECT 'ADMIN', 'Admin'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = seed.code);

-- Assign the supplied accounts and sample accounts 01–15 as leaners.
-- Sample accounts 16–25 are mentors. Re-running does not duplicate assignments.
INSERT INTO account_roles (account_id, role_id, assigned_at)
SELECT a.id, r.id, UTC_TIMESTAMP(6)
FROM accounts a
JOIN roles r ON r.code = 'LEANER'
WHERE (a.login_email IN (
    '0901dattran@gmail.com', 'letranbaophucxhb@gmail.com',
    'lengocminhkien06@gmail.com', 'tranquocthanhdat@gmail.com',
    'dattqde181019@fpt.edu.vn'
) OR a.login_email IN (
    'account01@example.com', 'account02@example.com', 'account03@example.com',
    'account04@example.com', 'account05@example.com', 'account06@example.com',
    'account07@example.com', 'account08@example.com', 'account09@example.com',
    'account10@example.com', 'account11@example.com', 'account12@example.com',
    'account13@example.com', 'account14@example.com', 'account15@example.com'
)) AND NOT EXISTS (
    SELECT 1 FROM account_roles ar WHERE ar.account_id = a.id AND ar.role_id = r.id
);

INSERT INTO account_roles (account_id, role_id, assigned_at)
SELECT a.id, r.id, UTC_TIMESTAMP(6)
FROM accounts a
JOIN roles r ON r.code = 'MENTOR'
WHERE a.login_email IN (
    'account16@example.com', 'account17@example.com', 'account18@example.com',
    'account19@example.com', 'account20@example.com', 'account21@example.com',
    'account22@example.com', 'account23@example.com', 'account24@example.com',
    'account25@example.com'
) AND NOT EXISTS (
    SELECT 1 FROM account_roles ar WHERE ar.account_id = a.id AND ar.role_id = r.id
);
