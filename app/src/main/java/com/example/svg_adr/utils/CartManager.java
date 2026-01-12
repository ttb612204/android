package com.example.svg_adr.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.svg_adr.model.CartItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * CartManager - Manages shopping cart using SharedPreferences
 */
public class CartManager {
    private static final String PREF_NAME = "svg_adr_cart";
    private static final String KEY_CART_ITEMS = "cart_items";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public CartManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }

    public void addItem(CartItem item) {
        List<CartItem> items = getItems();

        // Check if product already exists
        boolean found = false;
        for (CartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                found = true;
                break;
            }
        }

        if (!found) {
            items.add(item);
        }

        saveItems(items);
    }

    public void updateQuantity(String productId, int quantity) {
        List<CartItem> items = getItems();
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                if (quantity <= 0) {
                    items.remove(item);
                } else {
                    item.setQuantity(quantity);
                }
                break;
            }
        }
        saveItems(items);
    }

    public void removeItem(String productId) {
        List<CartItem> items = getItems();
        items.removeIf(item -> item.getProductId().equals(productId));
        saveItems(items);
    }

    public List<CartItem> getItems() {
        String json = prefs.getString(KEY_CART_ITEMS, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<CartItem>>() {
            }.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public List<CartItem> getItemsByStore(String storeId) {
        List<CartItem> allItems = getItems();
        List<CartItem> storeItems = new ArrayList<>();
        for (CartItem item : allItems) {
            if (item.getStoreId().equals(storeId)) {
                storeItems.add(item);
            }
        }
        return storeItems;
    }

    public void clearCart() {
        editor.remove(KEY_CART_ITEMS);
        editor.apply();
    }

    public void clearStoreItems(String storeId) {
        List<CartItem> items = getItems();
        items.removeIf(item -> item.getStoreId().equals(storeId));
        saveItems(items);
    }

    public int getItemCount() {
        List<CartItem> items = getItems();
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public double getTotal() {
        List<CartItem> items = getItems();
        double total = 0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    private void saveItems(List<CartItem> items) {
        String json = gson.toJson(items);
        editor.putString(KEY_CART_ITEMS, json);
        editor.apply();
    }
}
