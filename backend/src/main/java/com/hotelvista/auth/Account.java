package com.hotelvista.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false, length = 16)
    private String role;

    protected Account() { }
    Account(String fullName, String email, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = "CUSTOMER"; // Never accept a role from registration input.
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
}
