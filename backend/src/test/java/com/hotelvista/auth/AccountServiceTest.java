package com.hotelvista.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    @Test void savesHashAndCustomerRoleWithNormalizedIdentity() {
        var repo=mock(AccountRepository.class);
        var encoder=Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        when(repo.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var service=new AccountService(repo,encoder);
        var result=service.register(new RegisterRequest("  Pradeep  "," PERSON@EXAMPLE.COM ","safe sample password"));
        var captured=ArgumentCaptor.forClass(Account.class);verify(repo).saveAndFlush(captured.capture());
        assertThat(result.fullName()).isEqualTo("Pradeep");
        assertThat(result.email()).isEqualTo("person@example.com");
        assertThat(result.role()).isEqualTo("CUSTOMER");
        assertThat(captured.getValue().getPasswordHash()).isNotEqualTo("safe sample password");
        assertThat(encoder.matches("safe sample password",captured.getValue().getPasswordHash())).isTrue();
    }
    @Test void refusesDuplicateBeforeSaving() {
        var repo=mock(AccountRepository.class);when(repo.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        var service=new AccountService(repo,Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        assertThatThrownBy(() -> service.register(new RegisterRequest("Person","person@example.com","password123")))
            .isInstanceOf(ResponseStatusException.class);
        verify(repo,never()).saveAndFlush(any());
    }
}
