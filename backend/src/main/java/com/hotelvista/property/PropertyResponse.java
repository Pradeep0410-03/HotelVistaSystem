package com.hotelvista.property;

// Explicit response fields keep persistence internals out of the API.
public record PropertyResponse(Long id, String name, String propertyType, String city,
                               String address, String description, String timezone) {
    static PropertyResponse from(Property property) {
        return new PropertyResponse(property.getId(), property.getName(), property.getPropertyType(),
                property.getCity(), property.getAddress(), property.getDescription(), property.getTimezone());
    }
}
