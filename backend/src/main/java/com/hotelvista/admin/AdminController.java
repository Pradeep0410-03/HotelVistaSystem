package com.hotelvista.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/admin")
public class AdminController {
    public record Property(@NotBlank @Size(max=180) String name,
        @Pattern(regexp="HOTEL|RESORT|APARTMENT|HOMESTAY|VILLA|FARMHOUSE|BUNGALOW") @NotNull String propertyType,
        @NotBlank @Size(max=100) String city,@NotBlank @Size(max=2000) String address,
        @NotNull @Size(max=5000) String description,boolean active) {}
    public record Room(@NotBlank @Size(max=100) String name,@Min(1) @Max(100) int capacity,
        @Min(1) @Max(10000) int totalQuantity,@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price,boolean active) {}
    public record Inventory(@NotNull LocalDate start,@NotNull LocalDate end,@Min(0) @Max(10000) int quantity) {}
    public record Cancellation(@NotBlank @Size(max=500) String reason) {}
    private final AdminService service;
    public AdminController(AdminService service) { this.service=service; }
    @GetMapping("/properties") public Object properties(@RequestParam(defaultValue="") @Size(max=100) String city,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) { return service.properties(city,page); }
    @PostMapping("/properties") public Object add(@Valid @RequestBody Property data) { return service.saveProperty(null,data); }
    @PutMapping("/properties/{id}") public Object edit(@PathVariable @Positive long id,@Valid @RequestBody Property data) { return service.saveProperty(id,data); }
    @GetMapping("/properties/{id}/rooms") public Object rooms(@PathVariable @Positive long id) { return service.rooms(id); }
    @PostMapping("/properties/{id}/rooms") public Object addRoom(@PathVariable @Positive long id,@Valid @RequestBody Room data) { return service.saveRoom(id,null,data); }
    @PutMapping("/properties/{id}/rooms/{room}") public Object editRoom(@PathVariable @Positive long id,@PathVariable @Positive long room,@Valid @RequestBody Room data) { return service.saveRoom(id,room,data); }
    @GetMapping("/rooms/{id}/inventory") public Object inventory(@PathVariable @Positive long id,@RequestParam LocalDate start,@RequestParam LocalDate end) { return service.inventory(id,start,end); }
    @PutMapping("/rooms/{id}/inventory") public Object inventory(@PathVariable @Positive long id,@Valid @RequestBody Inventory data) { return service.inventory(id,data); }
    @GetMapping("/bookings") public Object bookings(@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) { return service.bookings(page); }
    @PostMapping("/bookings/{id}/cancel") public Object cancel(Principal principal,@PathVariable @Positive long id,@Valid @RequestBody Cancellation data) { return service.cancel(principal.getName(),id,data.reason()); }
}
