package com.hotelvista.auth;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;
    public AuthController(AccountService accounts) { this.accounts = accounts; }

    public record CsrfResponse(String headerName, String token) { }
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody RegisterRequest request) { return accounts.register(request); }

    @GetMapping("/me")
    public AccountResponse me(Principal principal) { return accounts.current(principal.getName()); }
    // Login/logout are handled by Spring Security filters, not hand-written controllers.
}
