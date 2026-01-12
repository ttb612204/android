package com.example.svg_adr.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Category implements Serializable {
    private String id;
    private String name;
    private String storeId;
    private Object createdAt;

    public Category() {
        // Required for Firestore
    }

    public Category(String name, String storeId) {
        this.name = name;
        this.storeId = storeId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("storeId", storeId);
        map.put("createdAt", createdAt);
        return map;
    }
}
