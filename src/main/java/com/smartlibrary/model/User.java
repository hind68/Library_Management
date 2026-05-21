package com.smartlibrary.model;

import java.time.LocalDateTime;

public class User {
    private final int id;
    private String fullName;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active;
    private String phone;
    private String address;
    private LocalDateTime createdAt;

    // Creates a User object with the supplied values.
    // Parameters: id is the database id; fullName is the user full name; email is the user email address
    // Parameters: passwordHash is the already-hashed password; role is the role being checked or assigned
    // Parameters: active is whether the account is enabled; createdAt is the date and time the record was created.
    public User(int id, String fullName, String email, String passwordHash, Role role, boolean active, LocalDateTime createdAt) {
        this(id, fullName, email, passwordHash, role, active, null, null, createdAt);
    }

    // Creates a User object with the supplied values.
    // Parameters: id is the database id; fullName is the user full name; email is the user email address
    // Parameters: passwordHash is the already-hashed password; role is the role being checked or assigned
    // Parameters: active is whether the account is enabled; phone is the user phone number; address is the user address
    // Parameters: createdAt is the date and time the record was created.
    public User(int id, String fullName, String email, String passwordHash, Role role, boolean active,
                String phone, String address, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.phone = phone;
        this.address = address;
        this.createdAt = createdAt;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the full name value.
    public String getFullName() { return fullName; }
    // Updates the full name value.
    // Parameters: fullName is the user full name.
    public void setFullName(String fullName) { this.fullName = fullName; }
    // Returns the email value.
    public String getEmail() { return email; }
    // Updates the email value.
    // Parameters: email is the user email address.
    public void setEmail(String email) { this.email = email; }
    // Returns the password hash value.
    public String getPasswordHash() { return passwordHash; }
    // Updates the password hash value.
    // Parameters: passwordHash is the already-hashed password.
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    // Returns the role value.
    public Role getRole() { return role; }
    // Updates the role value.
    // Parameters: role is the role being checked or assigned.
    public void setRole(Role role) { this.role = role; }
    // Returns whether active is true.
    public boolean isActive() { return active; }
    // Updates the active value.
    // Parameters: active is whether the account is enabled.
    public void setActive(boolean active) { this.active = active; }
    // Returns the phone value.
    public String getPhone() { return phone; }
    // Updates the phone value.
    // Parameters: phone is the user phone number.
    public void setPhone(String phone) { this.phone = phone; }
    // Returns the address value.
    public String getAddress() { return address; }
    // Updates the address value.
    // Parameters: address is the user address.
    public void setAddress(String address) { this.address = address; }
    // Returns the created at value.
    public LocalDateTime getCreatedAt() { return createdAt; }
    // Updates the created at value.
    // Parameters: createdAt is the date and time the record was created.
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
