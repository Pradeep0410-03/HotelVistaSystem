package com.hotelvista;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelvista.auth.*;
import com.hotelvista.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, AuthSecurityTest.AdminProbe.class})
@Import({SecurityConfig.class, AuthSecurityTest.FixtureUsers.class, AuthSecurityTest.AdminProbe.class})
class AuthSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean AccountService accounts;
    static final String PASSWORD="test-password-only";
    @TestConfiguration
    static class FixtureUsers {
        @Bean UserDetailsService users(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                User.withUsername("customer@example.invalid").password(encoder.encode(PASSWORD)).roles("CUSTOMER").build(),
                User.withUsername("admin@example.invalid").password(encoder.encode(PASSWORD)).roles("ADMIN").build());
        }
    }
    // A test-only route proves the admin matcher; no admin API is shipped yet.
    @RestController static class AdminProbe {
        @GetMapping("/api/admin/probe") String probe() { return "allowed"; }
    }
    record Token(MockHttpSession session, String header, String value) { }
    Token csrf(MockHttpSession session) throws Exception {
        var request=get("/api/auth/csrf");
        if(session!=null)request.session(session);
        var result=mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body=json.readTree(result.getResponse().getContentAsString());
        return new Token((MockHttpSession)result.getRequest().getSession(),body.get("headerName").asText(),body.get("token").asText());
    }
    MockHttpSession login(String email) throws Exception {
        var token=csrf(null);
        return (MockHttpSession)mvc.perform(post("/api/auth/login").session(token.session()).header(token.header(),token.value())
            .param("email",email).param("password",PASSWORD)).andExpect(status().isNoContent()).andReturn().getRequest().getSession();
    }
    @Test void rejectsAnonymousMeAndCsrfFreeMutations() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        for(String route : new String[]{"register","login","logout"}) {
            mvc.perform(post("/api/auth/"+route)).andExpect(status().isForbidden());
        }
    }
    @Test void loginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        given(accounts.current("customer@example.invalid")).willReturn(new AccountResponse(1L,"Customer","customer@example.invalid","CUSTOMER"));
        var token=csrf(null);String oldId=token.session().getId();
        var result=mvc.perform(post("/api/auth/login").session(token.session()).header(token.header(),token.value())
            .param("email","customer@example.invalid").param("password",PASSWORD)).andExpect(status().isNoContent()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession();
        assertThat(session.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("CUSTOMER")).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/logout").session(session).header(token.header(),token.value())).andExpect(status().isForbidden());
        var fresh=csrf(session);
        mvc.perform(post("/api/auth/logout").session(session).header(fresh.header(),fresh.value())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void wrongPasswordAndUnknownAccountReturnSameFailure() throws Exception {
        for(String email : new String[]{"customer@example.invalid","unknown@example.invalid"}) {
            var token=csrf(null);
            mvc.perform(post("/api/auth/login").session(token.session()).header(token.header(),token.value())
                .param("email",email).param("password","wrong-password"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
            mvc.perform(get("/api/auth/me").session(token.session())).andExpect(status().isUnauthorized());
        }
    }
    @Test void customerIsDeniedAdminRouteAndAdminIsAllowed() throws Exception {
        mvc.perform(get("/api/admin/probe").session(login("customer@example.invalid"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/probe").session(login("admin@example.invalid"))).andExpect(status().isOk());
    }
    @Test void rejectsInvalidRegistrationWithoutEchoingPassword() throws Exception {
        var token=csrf(null);
        mvc.perform(post("/api/auth/register").session(token.session()).header(token.header(),token.value())
                .contentType("application/json").content("{\"fullName\":\" \",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("short"))));
    }
}
