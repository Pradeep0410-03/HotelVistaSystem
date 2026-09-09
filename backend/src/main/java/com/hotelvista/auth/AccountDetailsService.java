package com.hotelvista.auth;

import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final AccountRepository accounts;
    public AccountDetailsService(AccountRepository accounts) { this.accounts = accounts; }
    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        var account = accounts.findByEmailIgnoreCase(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(account.getEmail()).password(account.getPasswordHash())
                .roles(account.getRole()).build();
    }
}
