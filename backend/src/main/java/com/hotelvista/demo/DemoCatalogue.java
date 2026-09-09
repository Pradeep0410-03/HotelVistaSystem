package com.hotelvista.demo;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit opt-in demo data. Flyway owns schema; this loader only inserts missing fixtures. */
@Component @Profile("demo")
public class DemoCatalogue implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public DemoCatalogue(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // Serializes concurrent demo starts without changing booking locks.
        jdbc.execute("SELECT pg_advisory_xact_lock(741852963)");
        String sql=new ClassPathResource("db/demo/catalogue.sql").getContentAsString(StandardCharsets.UTF_8);
        jdbc.execute(sql);
    }
}
