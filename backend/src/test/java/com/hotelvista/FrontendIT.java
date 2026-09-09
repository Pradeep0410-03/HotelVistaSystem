package com.hotelvista;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendIT {
    @Autowired MockMvc mvc;
    @Test void servesAccountPagesAndClientAlongsideApi() throws Exception {
        for(String page : new String[]{"index.html","login.html","register.html"}) {
            mvc.perform(get("/"+page)).andExpect(status().isOk()).andExpect(content().string(containsString("js/auth-client.js")));
        }
        mvc.perform(get("/bookings.html")).andExpect(status().isOk()).andExpect(content().string(containsString("js/bookings.js")));
        mvc.perform(get("/js/auth-client.js")).andExpect(status().isOk()).andExpect(content().string(containsString("credentials: 'same-origin'")));
        mvc.perform(get("/css/account.css")).andExpect(status().isOk());
        mvc.perform(get("/assets/optimized/destinations-udaipur.webp")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void doesNotExposeBackendFilesOrAllowStaticWrites() throws Exception {
        mvc.perform(post("/index.html")).andExpect(status().isForbidden());
        for(String path : new String[]{"/pom.xml","/application.yml","/docs/authentication.md","/backend/README.md"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
