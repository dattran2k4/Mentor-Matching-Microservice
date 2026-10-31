package com.mentormatching.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import com.mentormatching.auth.account.entity.*;
import com.mentormatching.auth.account.repository.*;
import com.mentormatching.auth.exception.*;
import com.mentormatching.auth.role.entity.AccountRole;
import com.mentormatching.auth.role.repository.*;
import com.mentormatching.auth.security.AccountUserDetailsService.AccountPrincipal;
import com.mentormatching.auth.service.dto.*;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai nghiệp vụ đăng nhập.
 */
@Service
public class LoginUseCase {

    private final AccountRepository accounts;
    private final AccountRoleRepository accountRoles;
    private final Validator validator;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public LoginUseCase(AccountRepository accounts, AccountRoleRepository accountRoles, Validator validator,
                        TokenService tokenService, AuthenticationManager authenticationManager) {
        this.accounts = accounts;
        this.accountRoles = accountRoles;
        this.validator = validator;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Xác thực mật khẩu và cấp token cho tài khoản đang hoạt động.
     *
     * @param command thông tin đăng nhập
     * @return cặp token đã phát hành
     */
    @Transactional
    public TokenService.IssuedTokens login(LoginCommand command) {

        if (command == null || command.email() == null || command.password() == null) {
            throw invalidCredentials();
        }

        String email = command.email().trim().toLowerCase(Locale.ROOT);

        if (!validator.validate(new LoginCommand(email, command.password())).isEmpty()
                || command.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, command.password()));
        } catch (AuthenticationException ex) {
            throw invalidCredentials();
        }

        // Get principle account from Authentication
        AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();

        Account account = accounts.findById(principal.getAccountId()).orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists"));

        var roleCodes = accountRoles.findByAccount_Id(account.getId())
                .stream()
                .map(assignment -> assignment.getRole().getCode()).toList();

        return tokenService.issue(account, roleCodes);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid credentials or account unavailable.");
    }

}
