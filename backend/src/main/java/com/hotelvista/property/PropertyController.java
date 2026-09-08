package com.hotelvista.property;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties")
@Validated
public class PropertyController {
    private final PropertyService service;
    public PropertyController(PropertyService service) { this.service = service; }

    @GetMapping
    public PropertyPage list(@RequestParam(defaultValue = "") @Size(max = 100) String city,
                             @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
                             @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return service.list(city, page, size);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable @Positive long id) { return service.get(id); }
}
