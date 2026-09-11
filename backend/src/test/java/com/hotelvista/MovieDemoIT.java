package com.hotelvista;
import com.hotelvista.movie.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @Transactional
class MovieDemoIT {
 @Autowired JdbcTemplate db; @Autowired MovieService service;
 @Test void repeatedDemoSeedingPreservesSeatClaims(){
  var seed=new MovieDemoCatalogue(db);var args=new DefaultApplicationArguments(new String[0]);seed.run(args);
  assertThat(db.queryForObject("SELECT count(*) FROM movie_shows WHERE demo_key LIKE 'vista-movie-%'",Integer.class)).isEqualTo(9);
  assertThat(db.queryForObject("SELECT count(*) FROM movie_show_seats s JOIN movie_shows m ON m.id=s.show_id WHERE m.demo_key LIKE 'vista-movie-%'",Integer.class)).isEqualTo(360);
  String email="movie-demo-"+UUID.randomUUID()+"@example.test";
  db.update("INSERT INTO users(full_name,email,password_hash) VALUES ('Test',?,'not-a-login-hash')",email);
  long show=db.queryForObject("SELECT min(id) FROM movie_shows WHERE demo_key LIKE 'vista-movie-%'",Long.class);
  var b=service.book(email,new MovieController.BookRequest(show,List.of("A1"),new BigDecimal("250"),UUID.randomUUID()));
  seed.run(args);
  assertThat(db.queryForObject("SELECT booking_id FROM movie_show_seats WHERE show_id=? AND label='A1'",Long.class,show)).isEqualTo(((Number)b.get("id")).longValue());
  assertThat(db.queryForObject("SELECT count(*) FROM movie_shows WHERE demo_key LIKE 'vista-movie-%'",Integer.class)).isEqualTo(9);
 }
}
