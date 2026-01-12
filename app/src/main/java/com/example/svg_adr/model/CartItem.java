package com.example.svg_adr.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CartItem - Represents an item in the shopping cart
 * Stored locally, not synced to Firestore until order is placed
 */
public class CartItem implements Serializable {
    private String productId;
    private String storeId;
    private String storeName;
    private String productName;
    private String productImage;
    private double price;
    private int quantity;

    public CartItem() {
    }

    public CartItem(Product product, Store store, int quantity) {
        this.productId = product.getId();
        this.storeId = product.getStoreId();
        this.storeName = store != null ? store.getName() : "";
        this.productName = product.getName();
        this.productImage = product.getImageUrl();
        this.price = product.getPrice();
        this.quantity = quantity;
    }

    // Convert to OrderItem
    public Order.OrderItem toOrderItem() {
        return new Order.OrderItem(productId, productName, productImage, price, quantity);
    }

    // Getters and Setters
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

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return price * quantity;
    }

    public String getFormattedPrice() {
        return String.format("%,.0f₫", price);
    }

    public String getFormattedSubtotal() {
        return String.format("%,.0f₫", getSubtotal());
    }
}
