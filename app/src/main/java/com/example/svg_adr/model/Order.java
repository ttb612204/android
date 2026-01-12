package com.example.svg_adr.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Order implements Serializable {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_SHIPPING = "shipping";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    private String id;
    private String userId;
    private String userName;
    private String userPhone;
    private String storeId;
    private String storeName;
    private List<OrderItem> items;
    private double totalAmount;
    private String status;
    private String address;
    private String paymentMethod; // "cod", "online"
    private String note;
    private long createdAt;
    private long updatedAt;
    private boolean isRated;

    public Order() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.status = STATUS_PENDING;
        this.items = new ArrayList<>();
        this.isRated = false;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userPhone", userPhone);
        map.put("storeId", storeId);
        map.put("storeName", storeName);
        map.put("totalAmount", totalAmount);
        map.put("status", status);
        map.put("address", address);
        map.put("paymentMethod", paymentMethod);
        map.put("note", note);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("isRated", isRated);

        // Convert items to list of maps
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        if (items != null) {
            for (OrderItem item : items) {
                itemMaps.add(item.toMap());
            }
        }
        map.put("items", itemMaps);
        return map;
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

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isRated() {
        return isRated;
    }

    public void setRated(boolean rated) {
        isRated = rated;
    }

    // Helper methods
    public String getFormattedTotal() {
        return String.format("%,.0f₫", totalAmount);
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }

    public String getStatusText() {
        switch (status) {
            case STATUS_PENDING:
                return "Chờ xác nhận";
            case STATUS_CONFIRMED:
                return "Đã xác nhận";
            case STATUS_SHIPPING:
                return "Đang giao";
            case STATUS_COMPLETED:
                return "Hoàn thành";
            case STATUS_CANCELLED:
                return "Đã hủy";
            default:
                return status;
        }
    }

    public boolean canCancel() {
        return STATUS_PENDING.equals(status);
    }

    public boolean canConfirm() {
        return STATUS_PENDING.equals(status);
    }

    public boolean canShip() {
        return STATUS_CONFIRMED.equals(status);
    }

    public boolean canComplete() {
        return STATUS_SHIPPING.equals(status);
    }

    public void addItem(OrderItem item) {
        if (items == null)
            items = new ArrayList<>();
        items.add(item);
        calculateTotal();
    }

    public void calculateTotal() {
        totalAmount = 0;
        if (items != null) {
            for (OrderItem item : items) {
                totalAmount += item.getSubtotal();
            }
        }
    }

    // Inner class for order items
    public static class OrderItem implements Serializable {
        private String productId;
        private String productName;
        private String productImage;
        private double price;
        private int quantity;

        public OrderItem() {
        }

        public OrderItem(String productId, String productName, String productImage, double price, int quantity) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.price = price;
            this.quantity = quantity;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", productId);
            map.put("productName", productName);
            map.put("productImage", productImage);
            map.put("price", price);
            map.put("quantity", quantity);
            return map;
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
}
