package com.hotelvista.booking;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    public record Booking(long id,String reference,String propertyName,String roomName,
        long propertyId,long roomTypeId,LocalDate checkin,LocalDate checkout,int guests,int rooms,
        BigDecimal nightlyPrice,BigDecimal totalAmount,String currency,String status,
        Instant cancellationDeadline,String acceptedPolicy) {}
    public record Page(List<Booking> items,int page,boolean hasNext) {}
    private record Room(long id,int capacity,int total,BigDecimal price,boolean active) {}
    private record Night(LocalDate date,int sellable,int reserved) {}
    private final JdbcTemplate jdbc;
    public BookingService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private BookingFailure fail(HttpStatus status,String message) { return new BookingFailure(status,message); }
    private long user(String email,boolean lock) {
        var ids=jdbc.query("SELECT id FROM users WHERE lower(email)=lower(?)"+(lock?" FOR UPDATE":""),(r,n)->r.getLong(1),email);
        if(ids.isEmpty())throw fail(HttpStatus.UNAUTHORIZED,"Sign in required");
        return ids.get(0);
    }
    private static final String SELECT = """
        SELECT b.*, p.name AS property_name, r.name AS room_name, i.room_type_id, i.quantity,
               i.agreed_nightly_price FROM bookings b
        JOIN properties p ON p.id=b.property_id JOIN booking_items i ON i.booking_id=b.id
        JOIN room_types r ON r.id=i.room_type_id
        """;
    private Booking map(ResultSet r,int row) throws SQLException {
        return new Booking(r.getLong("id"),r.getString("reference"),r.getString("property_name"),r.getString("room_name"),
            r.getLong("property_id"),r.getLong("room_type_id"),r.getDate("checkin").toLocalDate(),r.getDate("checkout").toLocalDate(),
            r.getInt("guest_count"),r.getInt("quantity"),r.getBigDecimal("agreed_nightly_price"),r.getBigDecimal("total_amount"),
            r.getString("currency"),r.getString("status"),r.getTimestamp("cancellation_deadline").toInstant(),r.getString("accepted_policy"));
    }
    private Booking get(long uid,long id) {
        var rows=jdbc.query(SELECT+" WHERE b.user_id=? AND b.id=?",this::map,uid,id);
        if(rows.isEmpty())throw fail(HttpStatus.NOT_FOUND,"Booking not found");
        return rows.get(0);
    }
    private Room room(long propertyId,long roomId) {
        var rows=jdbc.query("SELECT id,capacity,total_quantity,base_nightly_price,active FROM room_types WHERE property_id=? AND id=? FOR UPDATE",
            (r,n)->new Room(r.getLong(1),r.getInt(2),r.getInt(3),r.getBigDecimal(4),r.getBoolean(5)),propertyId,roomId);
        if(rows.isEmpty())throw fail(HttpStatus.NOT_FOUND,"Room type not found");
        return rows.get(0);
    }
    private List<Night> nights(long room,LocalDate in,LocalDate out) {
        return jdbc.query("SELECT stay_date,sellable_quantity,reserved_quantity FROM room_inventory WHERE room_type_id=? AND stay_date>=? AND stay_date<? ORDER BY stay_date FOR UPDATE",
            (r,n)->new Night(r.getDate(1).toLocalDate(),r.getInt(2),r.getInt(3)),room,java.sql.Date.valueOf(in),java.sql.Date.valueOf(out));
    }
    @Transactional
    public Booking create(String email,BookingController.Request request) {
        // Serialize same-account retries, then lock property, room and nights in a fixed order.
        long uid=user(email,true);
        var retries=jdbc.query(SELECT+" WHERE b.user_id=? AND b.request_key=?",this::map,uid,request.requestKey());
        if(!retries.isEmpty()) {
            Booking old=retries.get(0);
            if(old.propertyId()!=request.propertyId() || old.roomTypeId()!=request.roomTypeId() || !old.checkin().equals(request.checkin())
                || !old.checkout().equals(request.checkout()) || old.guests()!=request.guests() || old.rooms()!=request.rooms()
                || old.totalAmount().compareTo(request.expectedSubtotal())!=0)
                throw fail(HttpStatus.CONFLICT,"This request key was already used for a different booking");
            return old;
        }
        var zones=jdbc.query("SELECT timezone FROM properties WHERE id=? AND active FOR SHARE",(r,n)->r.getString(1),request.propertyId());
        if(zones.isEmpty())throw fail(HttpStatus.NOT_FOUND,"Property not found");
        var zone=ZoneId.of(zones.get(0));
        var today=LocalDate.now(zone);
        long count=ChronoUnit.DAYS.between(request.checkin(),request.checkout());
        if(request.checkin().isBefore(today) || request.checkout().isAfter(today.plusDays(365)) || count<1 || count>30)
            throw fail(HttpStatus.BAD_REQUEST,"Choose 1–30 nights, with no past check-in and checkout within 365 days");
        Room room=room(request.propertyId(),request.roomTypeId());
        if(!room.active())throw fail(HttpStatus.CONFLICT,"This room type is no longer available");
        if((long)room.capacity()*request.rooms()<request.guests())throw fail(HttpStatus.CONFLICT,"The selected rooms cannot accommodate these guests");
        var nights=nights(room.id(),request.checkin(),request.checkout());
        if(nights.size()!=count || nights.stream().anyMatch(n->Math.min(n.sellable(),room.total())-n.reserved()<request.rooms()))
            throw fail(HttpStatus.CONFLICT,"Rooms are no longer available for every night. Check availability again");
        var total=room.price().multiply(BigDecimal.valueOf(count)).multiply(BigDecimal.valueOf(request.rooms()));
        if(total.compareTo(request.expectedSubtotal())!=0)throw fail(HttpStatus.CONFLICT,"The price changed. Check availability and review the new price");
        String policy="Pay at hotel. Free cancellation before the check-in date in the property's local time; later requests require admin review.";
        long id=jdbc.queryForObject("""
            INSERT INTO bookings(reference,user_id,property_id,checkin,checkout,guest_count,total_amount,cancellation_deadline,accepted_policy,request_key)
            VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id
            """,Long.class,"HV-"+UUID.randomUUID(),uid,request.propertyId(),java.sql.Date.valueOf(request.checkin()),java.sql.Date.valueOf(request.checkout()),
            request.guests(),total,Timestamp.from(request.checkin().atStartOfDay(zone).toInstant()),policy,request.requestKey());
        jdbc.update("INSERT INTO booking_items(booking_id,property_id,room_type_id,quantity,agreed_nightly_price,subtotal) VALUES (?,?,?,?,?,?)",
            id,request.propertyId(),room.id(),request.rooms(),room.price(),total);
        for(Night n:nights) jdbc.update("UPDATE room_inventory SET reserved_quantity=reserved_quantity+? WHERE room_type_id=? AND stay_date=?",request.rooms(),room.id(),java.sql.Date.valueOf(n.date()));
        return get(uid,id);
    }
    @Transactional(readOnly=true)
    public Page list(String email,int page) {
        var rows=jdbc.query(SELECT+" WHERE b.user_id=? ORDER BY b.id DESC LIMIT 21 OFFSET ?",this::map,user(email,false),page*20);
        return new Page(rows.stream().limit(20).toList(),page,rows.size()>20);
    }
    @Transactional
    public Booking cancel(String email,long id) { return cancelInternal(email,id,false); }
    @Transactional
    public Booking cancelAsAdmin(String email,long id) { return cancelInternal(email,id,true); }
    private Booking cancelInternal(String email,long id,boolean admin) {
        long uid=user(email,true);
        var booking=get(uid,id);
        if(booking.status().equals("CANCELLED")) return booking;
        room(booking.propertyId(),booking.roomTypeId());
        if(!booking.status().equals("CONFIRMED") || (!admin && !Instant.now().isBefore(booking.cancellationDeadline())))
            throw fail(HttpStatus.CONFLICT,"Free cancellation has ended. Contact the property for admin review");
        var nights=nights(booking.roomTypeId(),booking.checkin(),booking.checkout());
        if(nights.size()!=ChronoUnit.DAYS.between(booking.checkin(),booking.checkout()) || nights.stream().anyMatch(n->n.reserved()<booking.rooms()))
            throw fail(HttpStatus.CONFLICT,"Inventory needs review; cancellation was not completed");
        for(Night n:nights) jdbc.update("UPDATE room_inventory SET reserved_quantity=reserved_quantity-? WHERE room_type_id=? AND stay_date=?",booking.rooms(),booking.roomTypeId(),java.sql.Date.valueOf(n.date()));
        jdbc.update("UPDATE bookings SET status='CANCELLED' WHERE id=? AND user_id=?",id,uid);
        return get(uid,id);
    }
}
