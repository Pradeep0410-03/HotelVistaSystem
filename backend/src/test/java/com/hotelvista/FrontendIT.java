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
        for(String page : new String[]{"index.html","frontend/pages/login.html","frontend/pages/register.html","frontend/pages/hotels.html","frontend/pages/movies.html","frontend/pages/sports.html"}) {
            mvc.perform(get("/"+page)).andExpect(status().isOk()).andExpect(content().string(containsString("frontend/js/api/auth-client.js")));
        }
        mvc.perform(get("/frontend/pages/bookings.html")).andExpect(status().isOk()).andExpect(content().string(containsString("frontend/js/pages/bookings.js")));
        mvc.perform(get("/frontend/js/api/auth-client.js")).andExpect(status().isOk()).andExpect(content().string(containsString("credentials: 'same-origin'")));
        mvc.perform(get("/frontend/styles/vista.css")).andExpect(status().isOk());
        mvc.perform(get("/assets/categories/cinema.jpg")).andExpect(status().isOk());
        mvc.perform(get("/assets/categories/stadium.jpg")).andExpect(status().isOk());
        mvc.perform(get("/frontend/styles/travel.css")).andExpect(status().isOk());
        mvc.perform(get("/frontend/js/shared/stay-presentation.js")).andExpect(status().isOk());
        mvc.perform(get("/frontend/styles/account.css")).andExpect(status().isOk());
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
