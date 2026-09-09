package com.hotelvista.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    public AccountService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }
    @Transactional
    public AccountResponse register(RegisterRequest request) {
        if (accounts.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
        }
        // The database's unique index also handles concurrent registrations.
        return AccountResponse.from(accounts.saveAndFlush(new Account(request.fullName(), request.email(),
                encoder.encode(request.password()))));
    }
    @Transactional(readOnly = true)
    public AccountResponse current(String email) {
        return accounts.findByEmailIgnoreCase(email).map(AccountResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account unavailable"));
    }
}
