package com.hotelvista.room;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvailabilityRepository {
    public record Room(long id, String name, int capacity, int available, BigDecimal nightlyPrice) {}
    private final JdbcTemplate jdbc;
    public AvailabilityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Room> find(long propertyId, LocalDate checkin, LocalDate checkout, long nights, int guests, int rooms) {
        // Count every night. Missing inventory rows exclude the room type entirely.
        // Capacity and quantity refer to one room type per option, not mixed rooms.
        return jdbc.query("""
            SELECT r.id, r.name, r.capacity, r.base_nightly_price,
                   min(least(i.sellable_quantity, r.total_quantity) - i.reserved_quantity) AS available
            FROM room_types r JOIN room_inventory i ON i.room_type_id = r.id
            WHERE r.property_id = ? AND r.active
              AND i.stay_date >= ? AND i.stay_date < ?
              AND cast(r.capacity AS bigint) * ? >= ?
            GROUP BY r.id, r.name, r.capacity, r.base_nightly_price
            HAVING count(*) = ?
               AND min(least(i.sellable_quantity, r.total_quantity) - i.reserved_quantity) >= ?
            ORDER BY r.base_nightly_price, r.id
            """, (rs,row) -> new Room(rs.getLong("id"), rs.getString("name"), rs.getInt("capacity"),
                rs.getInt("available"), rs.getBigDecimal("base_nightly_price")),
            propertyId, Date.valueOf(checkin), Date.valueOf(checkout), rooms, guests, nights, rooms);
    }
}
