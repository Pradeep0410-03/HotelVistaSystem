package com.hotelvista.property;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository repository;
    public PropertyService(PropertyRepository repository) { this.repository = repository; }

    public PropertyPage list(String city, int page, int size) {
        var result = repository.findActive(switch(city.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "new delhi" -> "Delhi";
            case "bangalore" -> "Bengaluru";
            case "pondicherry" -> "Puducherry";
            case "mysore" -> "Mysuru";
            default -> city.strip();
        },
                PageRequest.of(page, size, Sort.by("id").ascending()));
        return new PropertyPage(result.getContent().stream().map(PropertyResponse::from).toList(),
                page, size, result.hasNext());
    }

    public PropertyResponse get(long id) {
        return repository.findByIdAndActiveTrue(id).map(PropertyResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }
}
