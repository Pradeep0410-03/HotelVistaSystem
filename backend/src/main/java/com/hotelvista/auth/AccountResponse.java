package com.hotelvista.auth;

public record AccountResponse(Long id, String fullName, String email, String role) {
    static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getFullName(), account.getEmail(), account.getRole());
    }
}
