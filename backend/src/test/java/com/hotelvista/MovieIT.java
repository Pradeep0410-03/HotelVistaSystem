package com.hotelvista;

import com.hotelvista.movie.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class MovieIT {
 @Autowired JdbcTemplate db; @Autowired MovieService service;
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 long movie,cinema,show,u1,u2;String email1,email2;
 @BeforeEach void setup(){
  email1="movie-"+UUID.randomUUID()+"@example.test";email2="movie-"+UUID.randomUUID()+"@example.test";
  u1=addUser(email1);u2=addUser(email2);
  movie=db.queryForObject("INSERT INTO movies(title,language,duration_minutes) VALUES ('Test Movie','Hindi',120) RETURNING id",Long.class);
  cinema=db.queryForObject("INSERT INTO cinemas(name,city,address) VALUES ('Test Cinema','MovieTest','Test') RETURNING id",Long.class);
  show=db.queryForObject("INSERT INTO movie_shows(movie_id,cinema_id,starts_at) VALUES (?,?,CURRENT_TIMESTAMP+interval '3 days') RETURNING id",Long.class,movie,cinema);
  for(String label:List.of("A1","A2"))db.update("INSERT INTO movie_show_seats(show_id,label,price) VALUES (?,?,250)",show,label);
 }
 long addUser(String email){return db.queryForObject("INSERT INTO users(full_name,email,password_hash) VALUES ('Test',?,'not-a-login-hash') RETURNING id",Long.class,email);}
 MovieController.BookRequest request(UUID key,String... seats){return new MovieController.BookRequest(show,List.of(seats),new BigDecimal(250*seats.length),key);}
 long id(Map<String,Object> b){return ((Number)b.get("id")).longValue();}
 @AfterEach void cleanup(){
  db.update("DELETE FROM movie_booking_seats WHERE show_id IN (SELECT id FROM movie_shows WHERE cinema_id=?)",cinema);
  db.update("DELETE FROM movie_show_seats WHERE show_id IN (SELECT id FROM movie_shows WHERE cinema_id=?)",cinema);
  db.update("DELETE FROM movie_bookings WHERE show_id IN (SELECT id FROM movie_shows WHERE cinema_id=?)",cinema);
  db.update("DELETE FROM movie_shows WHERE cinema_id=?",cinema);
  db.update("DELETE FROM cinemas WHERE id=?",cinema);db.update("DELETE FROM movies WHERE id=?",movie);
  db.update("DELETE FROM users WHERE id IN (?,?)",u1,u2);
 }
 @Test void browseIsPublicAndReservationsArePrivate() throws Exception {
  mvc.perform(get("/api/movies?city=movietest")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].title").value("Test Movie"));
  mvc.perform(get("/api/movies/"+movie+"/shows?city=MovieTest")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(show));
  mvc.perform(get("/api/movie-shows/"+show+"/seats")).andExpect(status().isOk()).andExpect(jsonPath("$[0].available").value(true)).andExpect(jsonPath("$[0].booking_id").doesNotExist());
  mvc.perform(get("/api/movie-bookings")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/movie-bookings").with(user(email1)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request(UUID.randomUUID(),"A1")))).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/movies").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden());
 }
 @Test void reservationRetryOwnershipAndCancellation() throws Exception {
  var req=request(UUID.randomUUID(),"A1","A2");
  String body=mvc.perform(post("/api/movie-bookings").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(req)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.total_amount").value(500)).andExpect(jsonPath("$.seats.length()").value(2)).andReturn().getResponse().getContentAsString();
  long id=json.readTree(body).get("id").asLong();
  assertThat(id(service.book(email1,req))).isEqualTo(id);
  assertThat(service.bookings(email2,0).items()).isEmpty();
  mvc.perform(post("/api/movie-bookings/"+id+"/cancel").with(user(email2)).with(csrf())).andExpect(status().isNotFound());
  service.cancel(email1,id);service.cancel(email1,id);
  assertThat(service.seats(show)).allSatisfy(seat->assertThat(seat.get("available")).isEqualTo(true));
  assertThat(service.book(email1,req).get("status")).isEqualTo("CANCELLED");
  assertThat(service.book(email2,request(UUID.randomUUID(),"A1")).get("status")).isEqualTo("CONFIRMED");
  assertThat(db.queryForObject("SELECT count(*) FROM movie_booking_seats WHERE booking_id=?",Integer.class,id)).isEqualTo(2);
 }
 @Test void invalidSeatsAndStalePriceRollBackWholeSelection() throws Exception {
  service.book(email2,request(UUID.randomUUID(),"A2"));
  assertThatThrownBy(()->service.book(email1,request(UUID.randomUUID(),"A1","A2"))).isInstanceOf(MovieFailure.class);
  assertThat(db.queryForObject("SELECT booking_id IS NULL FROM movie_show_seats WHERE show_id=? AND label='A1'",Boolean.class,show)).isTrue();
  var stale=new MovieController.BookRequest(show,List.of("A1"),BigDecimal.ONE,UUID.randomUUID());
  mvc.perform(post("/api/movie-bookings").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(stale)))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Price changed; review the latest total"));
  assertThatThrownBy(()->service.book(email1,request(UUID.randomUUID(),"Z9"))).isInstanceOf(MovieFailure.class);
  mvc.perform(post("/api/movie-bookings").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request(UUID.randomUUID(),"A1","A1")))).andExpect(status().isBadRequest());
  var empty=new MovieController.BookRequest(show,List.of(),BigDecimal.ZERO,UUID.randomUUID());
  mvc.perform(post("/api/movie-bookings").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(empty))).andExpect(status().isBadRequest());
  assertThat(service.bookings(email1,0).items()).isEmpty();
 }
 @Test void startedShowsAndChangedRetryAreRejected(){
  UUID key=UUID.randomUUID();long id=id(service.book(email1,request(key,"A1")));
  assertThatThrownBy(()->service.book(email1,request(key,"A2"))).isInstanceOf(MovieFailure.class);
  db.update("UPDATE movie_shows SET starts_at=CURRENT_TIMESTAMP-interval '1 minute' WHERE id=?",show);
  assertThatThrownBy(()->service.cancel(email1,id)).isInstanceOf(MovieFailure.class);
  assertThatThrownBy(()->service.book(email2,request(UUID.randomUUID(),"A2"))).isInstanceOf(MovieFailure.class);
  assertThat(service.shows(movie,"MovieTest",0).items()).isEmpty();
 }
 @Test void concurrentCustomersCannotClaimTheSameSeat() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try {
   List<Future<Boolean>> results=new ArrayList<>();
   for(String email:List.of(email1,email2))results.add(pool.submit(()->{gate.await();try{service.book(email,request(UUID.randomUUID(),"A1"));return true;}catch(MovieFailure e){if(e.status().value()!=409)throw e;return false;}}));
   gate.countDown();int successes=0;for(var result:results)if(result.get(15,TimeUnit.SECONDS))successes++;
   assertThat(successes).isEqualTo(1);
   assertThat(db.queryForObject("SELECT count(*) FROM movie_bookings WHERE show_id=?",Integer.class,show)).isEqualTo(1);
  }finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
 }
 @Test void simultaneousRetriesCreateOneReservation() throws Exception {
  var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);var req=request(UUID.randomUUID(),"A1");
  try {
   var a=pool.submit(()->{gate.await();return id(service.book(email1,req));});
   var b=pool.submit(()->{gate.await();return id(service.book(email1,req));});gate.countDown();
   assertThat(a.get(15,TimeUnit.SECONDS)).isEqualTo(b.get(15,TimeUnit.SECONDS));
  }finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
 }
 @Test void adminCanPublishASeatMapButDuplicateSeatsAreRejected() throws Exception {
  String body="{\"movieId\":"+movie+",\"cinemaId\":"+cinema+",\"startsAt\":\""+java.time.OffsetDateTime.now().plusDays(5)+"\",\"seats\":[{\"label\":\"B1\",\"price\":300}]}";
  mvc.perform(post("/api/admin/movies/shows").with(user(email1).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  String invalid=body.replace("[{\"label\":\"B1\",\"price\":300}]","[{\"label\":\"B1\",\"price\":300},{\"label\":\"B1\",\"price\":300}]");
  mvc.perform(post("/api/admin/movies/shows").with(user(email1).roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
 }
}
