package com.example.svg_adr.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class    Product implements Serializable {
    private String id;
    private String storeId;
    private String name;
    private String description;
    private String imageUrl;
    private double price;
    private String category;
    private float rating;
    private int ratingCount;
    private boolean isApproved;
    private boolean isAvailable;
    private int soldCount;
    private Object createdAt;

    public Product() {
        this.createdAt = System.currentTimeMillis();
        this.isApproved = false;
        this.isAvailable = true;
        this.rating = 0;
        this.ratingCount = 0;
        this.soldCount = 0;
    }

    public Product(String storeId, String name, String description, double price, String category) {
        this();
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("storeId", storeId);
        map.put("name", name);
        map.put("description", description);
        map.put("imageUrl", imageUrl);
        map.put("price", price);
        map.put("category", category);
        map.put("rating", rating);
        map.put("ratingCount", ratingCount);
        map.put("isApproved", isApproved);
        map.put("isAvailable", isAvailable);
        map.put("soldCount", soldCount);
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

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    @PropertyName("isApproved")
    public boolean isApproved() {
        return isApproved;
    }

    @PropertyName("isApproved")
    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    @PropertyName("isAvailable")
    public boolean isAvailable() {
        return isAvailable;
    }

    @PropertyName("isAvailable")
    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
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

    // Helper
    public String getFormattedPrice() {
        return String.format("%,.0f₫", price);
    }
}
