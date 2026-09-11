package com.hotelvista.movie;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.*;

@Service
public class MovieService {
    private final JdbcTemplate db;
    public static final String POLICY = "Demo reservation: no payment collected. Free cancellation before the screening starts.";
    public MovieService(JdbcTemplate db) { this.db=db; }
    public record Page(List<Map<String,Object>> items, int page, boolean hasNext) {}
    private Page page(List<Map<String,Object>> rows,int page) {
        return new Page(rows.subList(0,Math.min(20,rows.size())),page,rows.size()>20);
    }
    public Page movies(String city,int page) {
        return page(db.queryForList("""
            SELECT DISTINCT m.id,m.title,m.language,m.duration_minutes FROM movies m
            JOIN movie_shows s ON s.movie_id=m.id JOIN cinemas c ON c.id=s.cinema_id
            WHERE m.active AND s.active AND s.starts_at>clock_timestamp()
            AND (?='' OR lower(c.city)=lower(?)) ORDER BY m.title,m.id LIMIT 21 OFFSET ?
            """,city.trim(),city.trim(),page*20),page);
    }
    public Page shows(long movie,String city,int page) {
        return page(db.queryForList("""
            SELECT s.id,s.movie_id,s.starts_at,c.name AS cinema_name,c.city,c.address,c.timezone
            FROM movie_shows s JOIN movies m ON m.id=s.movie_id JOIN cinemas c ON c.id=s.cinema_id
            WHERE m.active AND s.active AND s.movie_id=? AND s.starts_at>clock_timestamp()
            AND (?='' OR lower(c.city)=lower(?)) ORDER BY s.starts_at,s.id LIMIT 21 OFFSET ?
            """,movie,city.trim(),city.trim(),page*20),page);
    }
    public List<Map<String,Object>> seats(long show) {
        if (!Boolean.TRUE.equals(db.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM movie_shows s JOIN movies m ON m.id=s.movie_id
            WHERE s.id=? AND s.active AND m.active AND s.starts_at>clock_timestamp())
            """,Boolean.class,show))) throw new MovieFailure(NOT_FOUND,"Screening unavailable");
        return db.queryForList("SELECT label,price,(booking_id IS NULL) AS available FROM movie_show_seats WHERE show_id=? ORDER BY left(label,1),substring(label,2)::int",show);
    }
    // All mutations take the owner lock, then the show lock. Retries are serialized per owner.
    private long owner(String email) {
        var ids=db.queryForList("SELECT id FROM users WHERE lower(email)=lower(?) FOR UPDATE",Long.class,email);
        if(ids.isEmpty())throw new MovieFailure(UNAUTHORIZED,"Sign in required");
        return ids.get(0);
    }
    private void lockShow(long show) {
        var rows=db.queryForList("SELECT id FROM movie_shows WHERE id=? FOR UPDATE",show);
        if(rows.isEmpty())throw new MovieFailure(NOT_FOUND,"Screening not found");
    }
    private void requireFuture(long show,boolean forBooking) {
        boolean open=Boolean.TRUE.equals(db.queryForObject("""
            SELECT s.starts_at>clock_timestamp() AND (NOT ? OR (s.active AND m.active))
            FROM movie_shows s JOIN movies m ON m.id=s.movie_id WHERE s.id=?
            """,Boolean.class,forBooking,show));
        if(!open)throw new MovieFailure(CONFLICT,"Screening has started or is unavailable");
    }
    @Transactional
    public Map<String,Object> book(String email,MovieController.BookRequest request) {
        var labels=new TreeSet<>(request.seats());
        if(labels.size()!=request.seats().size())throw new MovieFailure(BAD_REQUEST,"Choose each seat only once");
        long user=owner(email);
        String fingerprint=request.showId()+":"+String.join(",",labels)+":"+request.expectedTotal().stripTrailingZeros().toPlainString();
        var previous=db.queryForList("SELECT id,request_fingerprint FROM movie_bookings WHERE user_id=? AND request_key=?",user,request.requestKey());
        if(!previous.isEmpty()) {
            if(!fingerprint.equals(previous.get(0).get("request_fingerprint")))throw new MovieFailure(CONFLICT,"Request key was used for a different selection");
            return detail(((Number)previous.get(0).get("id")).longValue(),user);
        }
        lockShow(request.showId());requireFuture(request.showId(),true);
        Map<String,BigDecimal> prices=new LinkedHashMap<>();
        for(String label:labels) {
            var rows=db.queryForList("SELECT price,booking_id FROM movie_show_seats WHERE show_id=? AND label=? FOR UPDATE",request.showId(),label);
            if(rows.isEmpty() || rows.get(0).get("booking_id")!=null)throw new MovieFailure(CONFLICT,"One or more seats are unavailable; refresh the seat map");
            prices.put(label,(BigDecimal)rows.get(0).get("price"));
        }
        BigDecimal total=prices.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.compareTo(request.expectedTotal())!=0)throw new MovieFailure(CONFLICT,"Price changed; review the latest total");
        long id=db.queryForObject("""
            INSERT INTO movie_bookings(reference,user_id,show_id,request_key,request_fingerprint,total_amount,accepted_policy)
            VALUES (?,?,?,?,?,?,?) RETURNING id
            """,Long.class,"VM-"+UUID.randomUUID(),user,request.showId(),request.requestKey(),fingerprint,total,POLICY);
        for(var seat:prices.entrySet()) {
            db.update("INSERT INTO movie_booking_seats(booking_id,show_id,label,agreed_price) VALUES (?,?,?,?)",id,request.showId(),seat.getKey(),seat.getValue());
            db.update("UPDATE movie_show_seats SET booking_id=? WHERE show_id=? AND label=?",id,request.showId(),seat.getKey());
        }
        return detail(id,user);
    }
    private Map<String,Object> detail(long id,long user) {
        var rows=db.queryForList("""
            SELECT b.id,b.reference,b.show_id,b.status,b.total_amount,b.currency,b.accepted_policy,
            s.starts_at,m.title,c.name AS cinema_name,c.city,c.timezone
            FROM movie_bookings b JOIN movie_shows s ON s.id=b.show_id
            JOIN movies m ON m.id=s.movie_id JOIN cinemas c ON c.id=s.cinema_id
            WHERE b.id=? AND b.user_id=?
            """,id,user);
        if(rows.isEmpty())throw new MovieFailure(NOT_FOUND,"Reservation not found");
        var result=rows.get(0);
        result.put("seats",db.queryForList("SELECT label,agreed_price FROM movie_booking_seats WHERE booking_id=? ORDER BY label",id));
        return result;
    }
    @Transactional(readOnly=true)
    public Page bookings(String email,int page) {
        long user=db.queryForObject("SELECT id FROM users WHERE lower(email)=lower(?)",Long.class,email);
        var ids=db.queryForList("SELECT id FROM movie_bookings WHERE user_id=? ORDER BY id DESC LIMIT 21 OFFSET ?",Long.class,user,page*20);
        return new Page(ids.stream().limit(20).map(id->detail(id,user)).toList(),page,ids.size()>20);
    }
    @Transactional
    public Map<String,Object> cancel(String email,long id) {
        long user=owner(email);
        var rows=db.queryForList("SELECT show_id,status FROM movie_bookings WHERE id=? AND user_id=?",id,user);
        if(rows.isEmpty())throw new MovieFailure(NOT_FOUND,"Reservation not found");
        if("CANCELLED".equals(rows.get(0).get("status")))return detail(id,user);
        long show=((Number)rows.get(0).get("show_id")).longValue();
        lockShow(show);requireFuture(show,false);
        db.update("UPDATE movie_show_seats SET booking_id=NULL WHERE booking_id=?",id);
        db.update("UPDATE movie_bookings SET status='CANCELLED' WHERE id=?",id);
        return detail(id,user);
    }
}
