package com.hotelvista;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    private String email;
    private static final String PASSWORD="test-only-password-123";
    @BeforeEach void identity() { email="auth-test-"+UUID.randomUUID()+"@example.invalid"; }
    @AfterEach void cleanup() { jdbc.update("DELETE FROM users WHERE email=?",email); }
    record Token(MockHttpSession session,String header,String value) { }
    Token csrf(MockHttpSession session) throws Exception {
        var request=get("/api/auth/csrf");if(session!=null)request.session(session);
        var result=mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body=json.readTree(result.getResponse().getContentAsString());
        return new Token((MockHttpSession)result.getRequest().getSession(),body.get("headerName").asText(),body.get("token").asText());
    }
    @Test void registerLoginMeAndLogoutWithRealDatabase() throws Exception {
        var token=csrf(null);
        String registration=json.writeValueAsString(Map.of("fullName"," Test Customer ","email"," "+email.toUpperCase(Locale.ROOT)+" ","password",PASSWORD,"role","ADMIN"));
        mvc.perform(post("/api/auth/register").session(token.session()).header(token.header(),token.value())
                .contentType("application/json").content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.passwordHash").doesNotExist());
        String hash=jdbc.queryForObject("SELECT password_hash FROM users WHERE email=?",String.class,email);
        assertThat(hash).startsWith("{pbkdf2@SpringSecurity_v5_8}").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD,hash)).isTrue();
        mvc.perform(get("/api/auth/me").session(token.session())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").session(token.session()).header(token.header(),token.value())
                .contentType("application/json").content(registration)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email=?",Integer.class,email)).isEqualTo(1);
        mvc.perform(post("/api/auth/login").session(token.session()).header(token.header(),token.value())
                .param("email",email).param("password","incorrect")).andExpect(status().isUnauthorized());
        var signedIn=mvc.perform(post("/api/auth/login").session(token.session()).header(token.header(),token.value())
                .param("email"," "+email.toUpperCase(Locale.ROOT)+" ").param("password",PASSWORD))
                .andExpect(status().isNoContent()).andReturn();
        var session=(MockHttpSession)signedIn.getRequest().getSession();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test Customer"));
        mvc.perform(get("/api/admin/anything").session(session)).andExpect(status().isForbidden());
        var fresh=csrf(session);
        mvc.perform(post("/api/auth/logout").session(session).header(fresh.header(),fresh.value())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
