package com.mentormatching.auth.dto.response;

import java.util.UUID;
import com.mentormatching.auth.entity.AccountStatus;

/**
 * Kết quả đăng ký tài khoản.
 *
 * @param accountId định danh tài khoản
 * @param status trạng thái tài khoản
 */
public record RegisterResult(UUID accountId, AccountStatus status) {
}
