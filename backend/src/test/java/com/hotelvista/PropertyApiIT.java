package com.hotelvista;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Opt in with -Pintegration verify and a fresh disposable PostgreSQL database.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PropertyApiIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    long activeId;
    long inactiveId;

    @BeforeEach void fixtures() {
        activeId=insert("First stay",true);
        insert("Second stay",true);
        inactiveId=insert("Hidden stay",false);
    }
    long insert(String name, boolean active) {
        return jdbc.queryForObject("INSERT INTO properties(name,property_type,city,address,description,active) VALUES (?, 'HOTEL', 'Integration City', 'Test address', 'Test only', ?) RETURNING id",Long.class,name,active);
    }

    @Test void flywayAppliesV1() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success",Integer.class)).isEqualTo(1);
    }

    @Test void filtersCaseInsensitivelyAndPaginatesOnlyActiveProperties() throws Exception {
        mvc.perform(get("/api/properties").param("city"," integration CITY ").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(activeId)).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/properties").param("city","Integration City").param("size","1").param("page","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("Second stay"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test void returnsDetailButHidesInactiveAndUnknownIds() throws Exception {
        mvc.perform(get("/api/properties/"+activeId)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("First stay"));
        mvc.perform(get("/api/properties/"+inactiveId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/properties/9223372036854775807")).andExpect(status().isNotFound());
    }

    @Test void returnsAnEmptyPageForUnknownCity() throws Exception {
        mvc.perform(get("/api/properties").param("city","No such test city"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }
}
