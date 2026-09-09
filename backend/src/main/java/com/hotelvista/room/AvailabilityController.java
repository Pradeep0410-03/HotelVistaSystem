package com.hotelvista.room;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/properties/{propertyId}/availability")
public class AvailabilityController {
    private final AvailabilityService service;
    public AvailabilityController(AvailabilityService service) { this.service = service; }
    @GetMapping
    public AvailabilityService.Result search(@PathVariable @Positive long propertyId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkin,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate checkout,
            @RequestParam(defaultValue="2") @Min(1) @Max(100) int guests,
            @RequestParam(defaultValue="1") @Min(1) @Max(20) int rooms) {
        return service.search(propertyId,checkin,checkout,guests,rooms);
    }
}
