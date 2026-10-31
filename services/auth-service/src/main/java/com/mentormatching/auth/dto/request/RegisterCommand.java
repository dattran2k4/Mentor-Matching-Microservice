package com.mentormatching.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Thông tin cần thiết để đăng ký tài khoản.
 *
 * @param email           email đăng nhập
 * @param password        mật khẩu
 * @param confirmPassword mật khẩu xác nhận
 */
public record RegisterCommand(
        @NotBlank @Email @Size(max = 254) String email,

        @NotBlank @Size(min = 8) String password,
        
        @NotBlank String confirmPassword) {
}
