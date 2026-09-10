package com.hotelvista.admin;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import com.hotelvista.booking.*;

@Service
public class AdminService {
    private final JdbcTemplate db;
    private final BookingService bookings;
    public AdminService(JdbcTemplate db,BookingService bookings) { this.db=db;this.bookings=bookings; }
    private BookingFailure fail(String message) { return new BookingFailure(HttpStatus.CONFLICT,message); }
    private void property(long id) { if(db.queryForList("SELECT id FROM properties WHERE id=? FOR UPDATE",id).isEmpty())throw fail("Property not found"); }
    private int room(long id) {
        var rows=db.queryForList("SELECT total_quantity FROM room_types WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty())throw fail("Room type not found");
        return ((Number)rows.get(0).get("total_quantity")).intValue();
    }
    public Object properties(String city,int page) { return db.queryForList("SELECT * FROM properties WHERE (?='' OR lower(city)=lower(?)) ORDER BY id LIMIT 21 OFFSET ?",city.strip(),city.strip(),page*20); }
    @Transactional public Object saveProperty(Long id,AdminController.Property p) {
        if(id==null) id=db.queryForObject("INSERT INTO properties(name,property_type,city,address,description,active) VALUES (?,?,?,?,?,?) RETURNING id",Long.class,p.name().strip(),p.propertyType(),p.city().strip(),p.address().strip(),p.description(),p.active());
        else { property(id);db.update("UPDATE properties SET name=?,property_type=?,city=?,address=?,description=?,active=? WHERE id=?",p.name().strip(),p.propertyType(),p.city().strip(),p.address().strip(),p.description(),p.active(),id); }
        return Map.of("id",id);
    }
    public Object rooms(long id) { return db.queryForList("SELECT * FROM room_types WHERE property_id=? ORDER BY id",id); }
    @Transactional public Object saveRoom(long property,Long id,AdminController.Room r) {
        property(property);
        if(id==null)id=db.queryForObject("INSERT INTO room_types(property_id,name,capacity,total_quantity,base_nightly_price,active) VALUES (?,?,?,?,?,?) RETURNING id",Long.class,property,r.name().strip(),r.capacity(),r.totalQuantity(),r.price(),r.active());
        else {
            room(id);
            if(db.queryForList("SELECT id FROM room_types WHERE id=? AND property_id=?",id,property).isEmpty())throw fail("Room does not belong to this property");
            int maximum=db.queryForObject("SELECT coalesce(max(sellable_quantity),0) FROM room_inventory WHERE room_type_id=?",Integer.class,id);
            if(r.totalQuantity()<maximum)throw fail("Reduce nightly inventory first; room quantity cannot be below existing inventory");
            db.update("UPDATE room_types SET name=?,capacity=?,total_quantity=?,base_nightly_price=?,active=? WHERE id=?",r.name().strip(),r.capacity(),r.totalQuantity(),r.price(),r.active(),id);
        }
        return Map.of("id",id);
    }
    private void range(LocalDate start,LocalDate end) {
        long n=ChronoUnit.DAYS.between(start,end);
        if(n<1||n>366)throw new BookingFailure(HttpStatus.BAD_REQUEST,"Choose a range of 1–366 nights; end date is excluded");
    }
    public Object inventory(long id,LocalDate start,LocalDate end) { range(start,end);return db.queryForList("SELECT * FROM room_inventory WHERE room_type_id=? AND stay_date>=? AND stay_date<? ORDER BY stay_date",id,java.sql.Date.valueOf(start),java.sql.Date.valueOf(end)); }
    @Transactional public Object inventory(long id,AdminController.Inventory data) {
        range(data.start(),data.end());int total=room(id);
        if(data.quantity()>total)throw fail("Inventory cannot exceed room quantity");
        for(LocalDate d=data.start();d.isBefore(data.end());d=d.plusDays(1)) {
            var date=java.sql.Date.valueOf(d);
            int reserved=db.queryForObject("SELECT coalesce(max(reserved_quantity),0) FROM room_inventory WHERE room_type_id=? AND stay_date=?",Integer.class,id,date);
            if(data.quantity()<reserved)throw fail("Inventory cannot be below reservations on "+d);
            db.update("INSERT INTO room_inventory(room_type_id,stay_date,sellable_quantity) VALUES (?,?,?) ON CONFLICT(room_type_id,stay_date) DO UPDATE SET sellable_quantity=EXCLUDED.sellable_quantity",id,date,data.quantity());
        }
        return Map.of("message","Inventory saved");
    }
    public Object bookings(int page) { return db.queryForList("SELECT b.id,b.reference,b.status,b.checkin,b.checkout,b.total_amount,p.name AS property_name,u.full_name AS guest_name FROM bookings b JOIN properties p ON p.id=b.property_id JOIN users u ON u.id=b.user_id ORDER BY b.id DESC LIMIT 21 OFFSET ?",page*20); }
    @Transactional public Object cancel(String actor,long id,String reason) {
        var rows=db.queryForList("SELECT u.email FROM bookings b JOIN users u ON u.id=b.user_id WHERE b.id=?",id);
        if(rows.isEmpty())throw fail("Booking not found");
        var result=bookings.cancelAsAdmin((String)rows.get(0).get("email"),id);
        db.update("INSERT INTO admin_cancellations(booking_id,actor_email,reason) VALUES (?,?,?) ON CONFLICT(booking_id) DO NOTHING",id,actor,reason.strip());
        return result;
    }
}
