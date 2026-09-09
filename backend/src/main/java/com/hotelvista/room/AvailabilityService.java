package com.hotelvista.room;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hotelvista.property.PropertyService;

@Service
public class AvailabilityService {
    public record Option(long roomTypeId, String name, int capacityPerRoom, int availableRooms,
                         BigDecimal nightlyPrice, BigDecimal subtotal) {}
    public record Result(long propertyId, LocalDate checkin, LocalDate checkout, long nights,
                         int guests, int rooms, String currency, String timezone,
                         Instant cancellationDeadline, String cancellationPolicy, List<Option> options) {}
    private final PropertyService properties;
    private final AvailabilityRepository repository;
    public AvailabilityService(PropertyService properties, AvailabilityRepository repository) {
        this.properties = properties; this.repository = repository;
    }
    @Transactional(readOnly=true)
    public Result search(long propertyId, LocalDate checkin, LocalDate checkout, int guests, int rooms) {
        var property = properties.get(propertyId);
        var zone = ZoneId.of(property.timezone());
        var today = LocalDate.now(zone);
        long nights = ChronoUnit.DAYS.between(checkin,checkout);
        if (checkin.isBefore(today) || checkout.isAfter(today.plusDays(365)) || nights < 1 || nights > 30
                || guests < 1 || guests > 100 || rooms < 1 || rooms > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        var options = repository.find(propertyId,checkin,checkout,nights,guests,rooms).stream()
                .map(room -> new Option(room.id(),room.name(),room.capacity(),room.available(),room.nightlyPrice(),
                    room.nightlyPrice().multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(rooms))))
                .toList();
        return new Result(propertyId,checkin,checkout,nights,guests,rooms,"INR",property.timezone(),
                checkin.atStartOfDay(zone).toInstant(),
                "Pay at hotel. Free cancellation before the check-in date in the property's local time; later requests require admin review.", options);
    }
}
