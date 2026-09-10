package com.hotelvista;
import com.hotelvista.admin.*;
import com.hotelvista.booking.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class AdminIT {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate db; @Autowired AdminService admin; @Autowired BookingService bookings;
 @Test void permissionsAndCsrfAreEnforced() throws Exception {
  mvc.perform(get("/api/admin/properties")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/properties").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/properties").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(post("/api/admin/properties").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/properties").with(user("customer").roles("CUSTOMER")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(get("/admin.html")).andExpect(status().isOk());
 }
 @Test void adminCancellationIsAuditedAndInventoryIsRestoredOnce() {
  String email="admin-fixture-"+UUID.randomUUID()+"@example.test";
  db.update("INSERT INTO users(full_name,email,password_hash) VALUES ('Test',?,'fixture')",email);
  long p=db.queryForObject("INSERT INTO properties(name,property_type,city,address) VALUES ('Admin fixture','HOTEL','Test','Test') RETURNING id",Long.class);
  long r=db.queryForObject("INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price) VALUES (?,'Deluxe',2,2,1000) RETURNING id",Long.class,p);
  LocalDate start=LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(3),end=start.plusDays(2);
  admin.inventory(r,new AdminController.Inventory(start,end,2));
  var b=bookings.create(email,new BookingController.Request(p,r,start,end,2,1,new BigDecimal("2000"),UUID.randomUUID()));
  db.update("UPDATE bookings SET cancellation_deadline=CURRENT_TIMESTAMP-interval '1 day' WHERE id=?",b.id());
  assertThatThrownBy(()->bookings.cancel(email,b.id())).isInstanceOf(BookingFailure.class);
  admin.cancel("admin@example.test",b.id(),"Guest requested an exception");
  admin.cancel("admin@example.test",b.id(),"Retry");
  assertThat(db.queryForObject("SELECT sum(reserved_quantity) FROM room_inventory WHERE room_type_id=?",Integer.class,r)).isZero();
  assertThat(db.queryForObject("SELECT reason FROM admin_cancellations WHERE booking_id=?",String.class,b.id())).isEqualTo("Guest requested an exception");
 }
 @Test void inventoryCannotDropBelowReservations() throws Exception {
  long p=db.queryForObject("INSERT INTO properties(name,property_type,city,address) VALUES ('Inventory fixture','HOTEL','Test','Test') RETURNING id",Long.class);
  long r=db.queryForObject("INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price) VALUES (?,'Deluxe',2,2,1000) RETURNING id",Long.class,p);
  LocalDate d=LocalDate.now().plusDays(4);
  db.update("INSERT INTO room_inventory(room_type_id,stay_date,sellable_quantity,reserved_quantity) VALUES (?,?,2,1)",r,java.sql.Date.valueOf(d));
  mvc.perform(put("/api/admin/rooms/"+r+"/inventory").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"start\":\""+d+"\",\"end\":\""+d.plusDays(1)+"\",\"quantity\":0}")).andExpect(status().isConflict());
  assertThat(db.queryForObject("SELECT sellable_quantity FROM room_inventory WHERE room_type_id=?",Integer.class,r)).isEqualTo(2);
 }
}
