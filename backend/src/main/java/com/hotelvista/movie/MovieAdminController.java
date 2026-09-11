package com.hotelvista.movie;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.HttpStatus.*;

@RestController @RequestMapping("/api/admin/movies")
public class MovieAdminController {
    private final JdbcTemplate db;
    public MovieAdminController(JdbcTemplate db){this.db=db;}
    public record Movie(@NotBlank @Size(max=180) String title,@NotBlank @Size(max=40) String language,@Min(1) @Max(600) int durationMinutes){}
    public record Cinema(@NotBlank @Size(max=180) String name,@NotBlank @Size(max=100) String city,@NotBlank @Size(max=500) String address,@NotBlank @Size(max=64) String timezone){}
    public record Seat(@NotNull @Pattern(regexp="[A-Z][1-9][0-9]{0,2}") String label,@NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal price){}
    public record Show(@Positive long movieId,@Positive long cinemaId,@NotNull @Future OffsetDateTime startsAt,@NotNull @Size(min=1,max=500) List<@NotNull @Valid Seat> seats){}
    @PostMapping public Map<String,Long> movie(@Valid @RequestBody Movie r){
        return Map.of("id",db.queryForObject("INSERT INTO movies(title,language,duration_minutes) VALUES (?,?,?) RETURNING id",Long.class,r.title().trim(),r.language().trim(),r.durationMinutes()));
    }
    @PostMapping("/cinemas") public Map<String,Long> cinema(@Valid @RequestBody Cinema r){
        try {ZoneId.of(r.timezone());}catch(DateTimeException e){throw new MovieFailure(BAD_REQUEST,"Use a valid cinema timezone");}
        return Map.of("id",db.queryForObject("INSERT INTO cinemas(name,city,address,timezone) VALUES (?,?,?,?) RETURNING id",Long.class,r.name().trim(),r.city().trim(),r.address().trim(),r.timezone()));
    }
    @PostMapping("/shows") @Transactional public Map<String,Long> show(@Valid @RequestBody Show r){
        if(r.seats().stream().map(Seat::label).distinct().count()!=r.seats().size())throw new MovieFailure(BAD_REQUEST,"Seat labels must be unique within a screening");
        if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM movies WHERE id=? AND active) AND EXISTS(SELECT 1 FROM cinemas WHERE id=?)",Boolean.class,r.movieId(),r.cinemaId())))throw new MovieFailure(NOT_FOUND,"Movie or cinema not found");
        long id=db.queryForObject("INSERT INTO movie_shows(movie_id,cinema_id,starts_at) VALUES (?,?,?) RETURNING id",Long.class,r.movieId(),r.cinemaId(),r.startsAt());
        for(Seat seat:r.seats())db.update("INSERT INTO movie_show_seats(show_id,label,price) VALUES (?,?,?)",id,seat.label(),seat.price());
        return Map.of("id",id);
    }
}
