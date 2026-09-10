package com.hotelvista.auth;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record RegisterRequest(@NotBlank @Size(max = 100) String fullName,
                              @NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(min = 12, max = 128)
                              @Pattern(regexp = "(?s)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).*") String password) {
    public RegisterRequest {
        if (fullName != null) fullName = fullName.strip();
        if (email != null) email = email.strip().toLowerCase(Locale.ROOT);
        // Passwords are exact input: do not trim or normalize them.
    }
}
