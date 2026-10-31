package com.mentormatching.auth.service;

import com.mentormatching.auth.service.dto.LoginCommand;
import com.mentormatching.auth.service.dto.RegisterCommand;
import com.mentormatching.auth.service.dto.RegisterResult;

/**
 * Các nghiệp vụ xác thực của Auth Service.
 */
public interface AuthService {
    /**
     * Đăng ký tài khoản chờ xác minh email.
     *
     * @param command thông tin đăng ký
     * @return định danh và trạng thái tài khoản
     */
    RegisterResult register(RegisterCommand command);

    /**
     * Xác thực tài khoản và phát hành cặp token.
     *
     * @param command thông tin đăng nhập
     * @return cặp token và thời điểm hết hạn
     */
    TokenService.IssuedTokens login(LoginCommand command);
}
