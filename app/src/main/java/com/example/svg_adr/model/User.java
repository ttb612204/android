package com.example.svg_adr.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    private String id;
    private String email;
    private String name;
    private String phone;
    private String role; // "customer", "store", "admin"
    private String avatarUrl;
    private String address;
    private boolean isApproved;
    private Object createdAt; // Can be Long or Timestamp from Firestore

    // Constructors
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.isApproved = true;
    }

    public User(String email, String name, String role) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.isApproved = role.equals("customer"); // Customers auto-approved
        this.createdAt = System.currentTimeMillis();
    }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("email", email);
        map.put("name", name);
        map.put("phone", phone);
        map.put("role", role);
        map.put("avatarUrl", avatarUrl);
        map.put("address", address);
        map.put("isApproved", isApproved);
        map.put("createdAt", getCreatedAtLong());
        return map;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @PropertyName("isApproved")
    public boolean isApproved() {
        return isApproved;
    }

    @PropertyName("isApproved")
    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    // Helper to get createdAt as long
    public long getCreatedAtLong() {
        if (createdAt == null) {
            return System.currentTimeMillis();
        }
        if (createdAt instanceof Long) {
            return (Long) createdAt;
        }
        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate().getTime();
        }
        if (createdAt instanceof Number) {
            return ((Number) createdAt).longValue();
        }
        return System.currentTimeMillis();
    }

    // Helper methods
    public boolean isCustomer() {
        return "customer".equals(role);
    }

    public boolean isStoreOwner() {
        return "store".equals(role);
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
