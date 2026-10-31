package com.mentormatching.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import com.mentormatching.auth.entity.*;
import com.mentormatching.auth.repository.*;
import com.mentormatching.auth.exception.*;
import com.mentormatching.auth.entity.AccountRole;
import com.mentormatching.auth.repository.*;
import com.mentormatching.auth.service.AccountUserDetailsService.AccountPrincipal;
import com.mentormatching.auth.dto.request.*;
import com.mentormatching.auth.dto.response.*;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai nghiệp vụ đăng ký tài khoản.
 */
@Service
public class RegisterUseCase {

    private static final String DEFAULT_ROLE = "LEANER";
    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final RoleRepository roles;
    private final AccountRoleRepository accountRoles;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public RegisterUseCase(AccountRepository accounts, PasswordCredentialRepository credentials,
                           RoleRepository roles, AccountRoleRepository accountRoles, PasswordEncoder passwordEncoder, Validator validator) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.roles = roles;
        this.accountRoles = accountRoles;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    /**
     * Tạo tài khoản, mật khẩu đã hash và role mặc định trong cùng transaction.
     * Email được chuẩn hóa bằng cách bỏ khoảng trắng hai đầu và chuyển chữ thường.
     */
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

}
