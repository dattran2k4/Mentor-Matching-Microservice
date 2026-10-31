package com.mentormatching.auth.entity;

/**
 * Trạng thái tài khoản; email_verified_at lưu sự thật xác minh độc lập với khóa/vô hiệu hóa.
 */
public enum AccountStatus {
    /**
     * Tài khoản đang chờ xác minh email.
     */
    PENDING_VERIFICATION,
    /**
     * Tài khoản ở trạng thái hoạt động.
     */
    ACTIVE,
    /**
     * Tài khoản bị khóa; quy tắc mở khóa thuộc nghiệp vụ.
     */
    LOCKED,
    /**
     * Tài khoản bị vô hiệu hóa.
     */
    DISABLED
}
