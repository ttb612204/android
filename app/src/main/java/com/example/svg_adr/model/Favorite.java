package com.example.svg_adr.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Favorite model for user's favorite products
 */
public class Favorite {
    private String id;
    private String userId;
    private String productId;
    private String productName;
    private String productImage;
    private double productPrice;
    private String storeId;
    private String storeName;
    private long createdAt;

    public Favorite() {
        this.createdAt = System.currentTimeMillis();
    }

    public Favorite(String userId, Product product, Store store) {
        this.userId = userId;
        this.productId = product.getId();
        this.productName = product.getName();
        this.productImage = product.getImageUrl();
        this.productPrice = product.getPrice();
        this.storeId = store != null ? store.getId() : product.getStoreId();
        this.storeName = store != null ? store.getName() : "";
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(double productPrice) {
        this.productPrice = productPrice;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedPrice() {
        return String.format("%,.0f₫", productPrice);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("productId", productId);
        map.put("productName", productName);
        map.put("productImage", productImage);
        map.put("productPrice", productPrice);
        map.put("storeId", storeId);
        map.put("storeName", storeName);
        map.put("createdAt", createdAt);
        return map;
    }
}
