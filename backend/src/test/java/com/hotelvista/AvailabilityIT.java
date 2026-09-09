package com.hotelvista;

import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class AvailabilityIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    long property, room;
    LocalDate start = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(10);
    @BeforeEach void fixture() {
        property = jdbc.queryForObject("INSERT INTO properties(name,property_type,city,address) VALUES ('Availability test','HOTEL','Test city','Test address') RETURNING id",Long.class);
        room = jdbc.queryForObject("INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price) VALUES (?,'Deluxe',2,10,1234.56) RETURNING id",Long.class,property);
        int[] reserved = {6,8,5};
        for(int i=0;i<3;i++) jdbc.update("INSERT INTO room_inventory(room_type_id,stay_date,sellable_quantity,reserved_quantity) VALUES (?,?,10,?)",room,java.sql.Date.valueOf(start.plusDays(i)),reserved[i]);
    }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(int guests,int rooms) {
        return get("/api/properties/"+property+"/availability").param("checkin",start.toString()).param("checkout",start.plusDays(3).toString()).param("guests",""+guests).param("rooms",""+rooms);
    }
    @Test void minimumNightAndExactSubtotalExcludingCheckout() throws Exception {
        mvc.perform(request(4,2)).andExpect(status().isOk()).andExpect(jsonPath("$.options[0].availableRooms").value(2))
            .andExpect(jsonPath("$.options[0].subtotal").value(7407.36)).andExpect(jsonPath("$.nights").value(3))
            .andExpect(jsonPath("$.cancellationDeadline").value(start.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().toString()));
        mvc.perform(request(6,3)).andExpect(status().isOk()).andExpect(jsonPath("$.options").isEmpty());
        mvc.perform(request(5,2)).andExpect(status().isOk()).andExpect(jsonPath("$.options").isEmpty());
    }
    @Test void missingNightAndInactiveRoomAreUnavailable() throws Exception {
        jdbc.update("UPDATE room_types SET active=false WHERE id=?",room);
        mvc.perform(request(2,1)).andExpect(status().isOk()).andExpect(jsonPath("$.options").isEmpty());
        jdbc.update("UPDATE room_types SET active=true WHERE id=?",room);
        jdbc.update("DELETE FROM room_inventory WHERE room_type_id=? AND stay_date=?",room,java.sql.Date.valueOf(start.plusDays(1)));
        mvc.perform(request(2,1)).andExpect(status().isOk()).andExpect(jsonPath("$.options").isEmpty());
        jdbc.update("UPDATE room_types SET active=false WHERE id=?",room);
        mvc.perform(request(2,1)).andExpect(status().isOk()).andExpect(jsonPath("$.options").isEmpty());
    }
    @Test void invalidRequestsAndInactivePropertyAreRejected() throws Exception {
        mvc.perform(request(0,1)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/properties/"+property+"/availability").param("checkin",start.toString()).param("checkout",start.toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/properties/"+property+"/availability").param("checkin",start.toString()).param("checkout",start.plusDays(31).toString())).andExpect(status().isBadRequest());
        jdbc.update("UPDATE properties SET active=false WHERE id=?",property);
        mvc.perform(request(2,1)).andExpect(status().isNotFound());
    }
}
