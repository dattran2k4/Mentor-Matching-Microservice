package com.mentormatching.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.mentormatching.auth.entity.AccountStatus;
import com.mentormatching.auth.repository.AccountRepository;
import com.mentormatching.auth.repository.PasswordCredentialRepository;
import com.mentormatching.auth.repository.AccountRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tải tài khoản và mật khẩu đã hash để Spring Security xác thực.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;
    private final PasswordCredentialRepository credentials;
    private final AccountRoleRepository accountRoles;

    public AccountUserDetailsService(AccountRepository accounts, PasswordCredentialRepository credentials,
                                     AccountRoleRepository accountRoles) {
        this.accounts = accounts;
        this.credentials = credentials;
        this.accountRoles = accountRoles;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Account
        var account = accounts.findByLoginEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        // Credential
        var credential = credentials.findById(account.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        // Role
        var authorities = accountRoles.findByAccount_Id(account.getId()).stream()
                .map(assignment -> new SimpleGrantedAuthority("ROLE_" + assignment.getRole().getCode()))
                .toList();
        
        return new AccountPrincipal(account.getId(), account.getLoginEmail(), credential.getPasswordHash(),
                account.getStatus() == AccountStatus.ACTIVE, authorities);
    }

    /**
     * Thông tin tài khoản dùng trong quá trình xác thực Spring Security.
     */
    public static class AccountPrincipal implements UserDetails, CredentialsContainer {
        private final UUID accountId;
        private final String email;
        private String passwordHash;
        private final boolean enabled;
        private final List<GrantedAuthority> authorities;

        public AccountPrincipal(UUID accountId, String email, String passwordHash, boolean enabled,
                                Collection<? extends GrantedAuthority> authorities) {
            this.accountId = accountId;
            this.email = email;
            this.passwordHash = passwordHash;
            this.enabled = enabled;
            this.authorities = List.copyOf(authorities);
        }

        public UUID getAccountId() {
            return accountId;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return passwordHash;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void eraseCredentials() {
            passwordHash = null;
        }
    }
}
