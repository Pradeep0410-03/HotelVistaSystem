package com.hotelvista;

import com.hotelvista.booking.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
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
class BookingIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired BookingService service;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    long property,room,u1,u2;
    String email1,email2;
    LocalDate start=LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(10);
    @BeforeEach void setup(){
        email1="booking-"+UUID.randomUUID()+"@example.test";email2="booking-"+UUID.randomUUID()+"@example.test";
        u1=addUser(email1);u2=addUser(email2);
        property=jdbc.queryForObject("INSERT INTO properties(name,property_type,city,address) VALUES ('Booking fixture','HOTEL','Test','Test') RETURNING id",Long.class);
        room=jdbc.queryForObject("INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price) VALUES (?,'Deluxe',2,1,1234.56) RETURNING id",Long.class,property);
        for(int i=0;i<2;i++)jdbc.update("INSERT INTO room_inventory(room_type_id,stay_date,sellable_quantity) VALUES (?,?,1)",room,java.sql.Date.valueOf(start.plusDays(i)));
    }
    long addUser(String email){return jdbc.queryForObject("INSERT INTO users(full_name,email,password_hash) VALUES ('Test',?,'not-a-login-hash') RETURNING id",Long.class,email);}
    BookingController.Request request(UUID key){return new BookingController.Request(property,room,start,start.plusDays(2),2,1,new BigDecimal("2469.12"),key);}
    @AfterEach void cleanup(){
        jdbc.update("DELETE FROM booking_items WHERE property_id=?",property);
        jdbc.update("DELETE FROM bookings WHERE property_id=?",property);
        jdbc.update("DELETE FROM room_inventory WHERE room_type_id=?",room);
        jdbc.update("DELETE FROM room_types WHERE property_id=?",property);
        jdbc.update("DELETE FROM properties WHERE id=?",property);
        jdbc.update("DELETE FROM users WHERE id IN (?,?)",u1,u2);
    }
    @Test void createsOwnedBookingAndCancellationReleasesInventoryOnce() throws Exception {
        var request=request(UUID.randomUUID());
        String body=mvc.perform(post("/api/bookings").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(2469.12)).andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andReturn().getResponse().getContentAsString();
        long id=json.readTree(body).get("id").asLong();
        assertThat(service.create(email1,request).id()).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings WHERE property_id=?",Integer.class,property)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT sum(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,room)).isEqualTo(2);
        mvc.perform(get("/api/bookings").with(user(email2))).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(post("/api/bookings/"+id+"/cancel").with(user(email2)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/bookings/"+id+"/cancel").with(user(email1)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        service.cancel(email1,id);
        assertThat(jdbc.queryForObject("SELECT sum(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,room)).isZero();
        assertThat(service.list(email1,0).items()).hasSize(1);
    }
    @Test void changedPriceAndCsrfAndAnonymousAreRejected() throws Exception {
        var request=request(UUID.randomUUID());
        mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/bookings").with(user(email1)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isForbidden());
        jdbc.update("UPDATE room_types SET base_nightly_price=9999 WHERE id=?",room);
        mvc.perform(post("/api/bookings").with(user(email1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings WHERE property_id=?",Integer.class,property)).isZero();
        assertThat(jdbc.queryForObject("SELECT sum(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,room)).isZero();
    }
    @Test void twoCustomersRacingForLastRoomCannotOverbook() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(String email:List.of(email1,email2))results.add(pool.submit(()->{gate.await();try{service.create(email,request(UUID.randomUUID()));return true;}catch(BookingFailure e){if(e.status().value()!=409)throw e;return false;}}));
            gate.countDown();int success=0;for(var result:results)if(result.get(15,TimeUnit.SECONDS))success++;
            assertThat(success).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings WHERE property_id=?",Integer.class,property)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT max(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,room)).isEqualTo(1);
        }finally{pool.shutdownNow();pool.awaitTermination(5,TimeUnit.SECONDS);}
    }
    @Test void cancellationDeadlineAndChangedRetryPayloadAreEnforced(){
        UUID key=UUID.randomUUID();var b=service.create(email1,request(key));
        var different=new BookingController.Request(property,room,start,start.plusDays(2),1,1,new BigDecimal("2469.12"),key);
        assertThatThrownBy(()->service.create(email1,different)).isInstanceOf(BookingFailure.class);
        jdbc.update("UPDATE bookings SET cancellation_deadline=CURRENT_TIMESTAMP-interval '1 minute' WHERE id=?",b.id());
        assertThatThrownBy(()->service.cancel(email1,b.id())).isInstanceOf(BookingFailure.class);
        assertThat(jdbc.queryForObject("SELECT sum(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,room)).isEqualTo(2);
    }
}
