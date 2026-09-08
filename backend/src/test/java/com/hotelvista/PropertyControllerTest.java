package com.hotelvista;

import com.hotelvista.property.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PropertyController.class)
class PropertyControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PropertyService service;

    @Test void returnsAnExplicitPageContract() throws Exception {
        given(service.list("Goa", 0, 12)).willReturn(new PropertyPage(List.of(
                new PropertyResponse(1L,"Palm & Tide","RESORT","Goa","North Goa","Sample","Asia/Kolkata")),0,12,false));
        mvc.perform(get("/api/properties").param("city","Goa"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("Palm & Tide"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test void rejectsInvalidParametersBeforeCallingService() throws Exception {
        for (String query : List.of("size=0","size=51","page=-1","page=10001","page=nope")) {
            mvc.perform(get("/api/properties?"+query)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/properties").param("city","x".repeat(101))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/properties/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/properties/not-a-number")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void offersNoWriteEndpoint() throws Exception {
        mvc.perform(post("/api/properties").contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
