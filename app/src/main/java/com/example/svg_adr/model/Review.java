package com.example.svg_adr.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Review model for product/store reviews
 */
public class Review {
    private String id;
    private String userId;
    private String userName;
    private String productId;
    private String storeId;
    private String orderId;
    private float rating;
    private String comment;
    private long createdAt;

    public Review() {
        this.createdAt = System.currentTimeMillis();
    }

    public Review(String userId, String userName, String productId, String storeId,
            String orderId, float rating, String comment) {
        this.userId = userId;
        this.userName = userName;
        this.productId = productId;
        this.storeId = storeId;
        this.orderId = orderId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("productId", productId);
        map.put("storeId", storeId);
        map.put("orderId", orderId);
        map.put("rating", rating);
        map.put("comment", comment);
        map.put("createdAt", createdAt);
        return map;
    }
}
