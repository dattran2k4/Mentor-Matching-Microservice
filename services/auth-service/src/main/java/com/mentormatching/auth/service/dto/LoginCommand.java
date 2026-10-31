package com.mentormatching.auth.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Thông tin đăng nhập bằng mật khẩu.
 *
 * @param email    email đăng nhập
 * @param password mật khẩu
 */
public record LoginCommand(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank String password) {
}
