package com.hotelvista.movie;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api")
public class MovieController {
    private final MovieService service;
    public MovieController(MovieService service){this.service=service;}
    public record BookRequest(@Positive long showId,
        @NotNull @Size(min=1,max=10) List<@NotNull @Pattern(regexp="[A-Z][1-9][0-9]{0,2}") String> seats,
        @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal expectedTotal,
        @NotNull UUID requestKey) {}
    @GetMapping("/movies") public MovieService.Page movies(@RequestParam(defaultValue="") @Size(max=100) String city,
        @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return service.movies(city,page);}
    @GetMapping("/movies/{id}/shows") public MovieService.Page shows(@PathVariable @Positive long id,
        @RequestParam(defaultValue="") @Size(max=100) String city,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return service.shows(id,city,page);}
    @GetMapping("/movie-shows/{id}/seats") public List<Map<String,Object>> seats(@PathVariable @Positive long id){return service.seats(id);}
    @PostMapping("/movie-bookings") public Map<String,Object> book(Principal principal,@Valid @RequestBody BookRequest request){return service.book(principal.getName(),request);}
    @GetMapping("/movie-bookings") public MovieService.Page bookings(Principal principal,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return service.bookings(principal.getName(),page);}
    @PostMapping("/movie-bookings/{id}/cancel") public Map<String,Object> cancel(Principal principal,@PathVariable @Positive long id){return service.cancel(principal.getName(),id);}
}
