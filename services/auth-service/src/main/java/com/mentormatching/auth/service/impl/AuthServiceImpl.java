package com.mentormatching.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import com.mentormatching.auth.account.entity.Account;
import com.mentormatching.auth.account.entity.AccountStatus;
import com.mentormatching.auth.account.entity.PasswordCredential;
import com.mentormatching.auth.account.repository.AccountRepository;
import com.mentormatching.auth.account.repository.PasswordCredentialRepository;
import com.mentormatching.auth.exception.ApiException;
import com.mentormatching.auth.exception.ErrorCode;
import com.mentormatching.auth.role.entity.AccountRole;
import com.mentormatching.auth.role.repository.AccountRoleRepository;
import com.mentormatching.auth.role.repository.RoleRepository;
import com.mentormatching.auth.service.AuthService;
import com.mentormatching.auth.service.TokenService;
import com.mentormatching.auth.service.dto.LoginCommand;
import com.mentormatching.auth.service.dto.RegisterCommand;
import com.mentormatching.auth.service.dto.RegisterResult;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai nghiệp vụ đăng ký tài khoản.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "LEANER";

    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final RoleRepository roles;
    private final AccountRoleRepository accountRoles;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final TokenService tokenService;

    public AuthServiceImpl(AccountRepository accounts, PasswordCredentialRepository credentials,
                           RoleRepository roles, AccountRoleRepository accountRoles,
                           PasswordEncoder passwordEncoder, Validator validator, TokenService tokenService) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.roles = roles;
        this.accountRoles = accountRoles;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.tokenService = tokenService;
    }

    /**
     * Tạo tài khoản, mật khẩu đã hash và role mặc định trong cùng transaction.
     * Email được chuẩn hóa bằng cách bỏ khoảng trắng hai đầu và chuyển chữ thường.
     */
    @Override
    @Transactional
    public RegisterResult register(RegisterCommand command) {

        if (command == null || command.email() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REGISTRATION, "Invalid registration data.");
        }

        String email = command.email().trim().toLowerCase(Locale.ROOT);

        if (!validator.validate(new RegisterCommand(email, command.password(), command.confirmPassword())).isEmpty()
                || command.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REGISTRATION, "Invalid registration data.");
        }

        // check confirm password
        if (!command.password().equals(command.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.PASSWORD_MISMATCH, "Password confirmation does not match.");
        }

        // check mail existed
        if (accounts.findByLoginEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.EMAIL_ALREADY_EXISTS, "Email already exists.");
        }

        var role = roles.findByCode(DEFAULT_ROLE).orElseThrow(() -> new IllegalStateException("Default registration role is not configured"));

        Instant now = Instant.now();

        // Account
        Account account = new Account();
        account.setLoginEmail(email);
        account.setStatus(AccountStatus.PENDING_VERIFICATION);
        account = accounts.save(account);

        // PasswordCredential
        PasswordCredential credential = new PasswordCredential();
        credential.setAccount(account);
        credential.setPasswordHash(passwordEncoder.encode(command.password()));
        credential.setPasswordChangedAt(now);
        credentials.save(credential);

        // AccountRole
        AccountRole assignment = new AccountRole();
        assignment.setAccount(account);
        assignment.setRole(role);
        assignment.setAssignedAt(now);
        accountRoles.save(assignment);

        return new RegisterResult(account.getId(), account.getStatus());
    }

    /**
     * Xác thực mật khẩu và cấp token cho tài khoản đang hoạt động.
     *
     * @param command thông tin đăng nhập
     * @return cặp token đã phát hành
     */
    @Override
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

        Account account = accounts.findByLoginEmail(email).orElseThrow(AuthServiceImpl::invalidCredentials);
        PasswordCredential credential = credentials.findById(account.getId()).orElseThrow(AuthServiceImpl::invalidCredentials);

        if (!passwordEncoder.matches(command.password(), credential.getPasswordHash())
                || account.getStatus() != AccountStatus.ACTIVE) {
            throw invalidCredentials();
        }

        var roleCodes = accountRoles.findByAccount_Id(account.getId())
                .stream()
                .map(assignment -> assignment.getRole().getCode()).toList();

        return tokenService.issue(account, roleCodes);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid credentials or account unavailable.");
    }

}
