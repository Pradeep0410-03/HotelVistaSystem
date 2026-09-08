package com.hotelvista.property;

import jakarta.persistence.*;

// A read-only mapping for the first catalogue API; schema changes belong to Flyway.
@Entity
@Table(name = "properties")
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String name;
    @Column(name = "property_type", nullable = false, length = 24)
    private String propertyType;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, columnDefinition = "text")
    private String address;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(nullable = false, length = 64)
    private String timezone;
    @Column(nullable = false)
    private boolean active;

    protected Property() { }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPropertyType() { return propertyType; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public String getTimezone() { return timezone; }
}
