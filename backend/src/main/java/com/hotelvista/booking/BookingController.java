package com.hotelvista.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/bookings")
public class BookingController {
    public record Request(@Positive long propertyId, @Positive long roomTypeId,
        @NotNull LocalDate checkin, @NotNull LocalDate checkout,
        @Min(1) @Max(100) int guests, @Min(1) @Max(20) int rooms,
        @NotNull @DecimalMin("0.00") @Digits(integer=12,fraction=2) BigDecimal expectedSubtotal,
        @NotNull UUID requestKey) {}
    private final BookingService service;
    public BookingController(BookingService service) { this.service=service; }
    @PostMapping public BookingService.Booking create(Principal principal,@Valid @RequestBody Request request) {
        return service.create(principal.getName(),request);
    }
    @GetMapping public BookingService.Page list(Principal principal,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) {
        return service.list(principal.getName(),page);
    }
    @PostMapping("/{id}/cancel") public BookingService.Booking cancel(Principal principal,@PathVariable @Positive long id) {
        return service.cancel(principal.getName(),id);
    }
}
