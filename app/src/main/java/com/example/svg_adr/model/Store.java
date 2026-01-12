package com.example.svg_adr.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Store implements Serializable {
    private String id;
    private String ownerId;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String imageUrl;
    private boolean isApproved;
    private float rating;
    private int orderCount;
    private double totalRevenue;
    private Object createdAt;

    public Store() {
        this.createdAt = System.currentTimeMillis();
        this.isApproved = false;
        this.rating = 0;
        this.orderCount = 0;
        this.totalRevenue = 0;
    }

    public Store(String ownerId, String name, String description, String address, String phone) {
        this();
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("ownerId", ownerId);
        map.put("name", name);
        map.put("description", description);
        map.put("address", address);
        map.put("phone", phone);
        map.put("imageUrl", imageUrl);
        map.put("isApproved", isApproved);
        map.put("rating", rating);
        map.put("orderCount", orderCount);
        map.put("totalRevenue", totalRevenue);
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

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("isApproved")
    public boolean isApproved() {
        return isApproved;
    }

    @PropertyName("isApproved")
    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
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
}
