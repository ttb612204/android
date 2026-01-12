package com.example.svg_adr.utils;

/**
 * Constants - App-wide constants
 */
public class Constants {
    // Firestore Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_STORES = "stores";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_ORDERS = "orders";
    public static final String COLLECTION_FAVORITES = "favorites";
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_CATEGORIES = "categories";

    // User Roles
    public static final String ROLE_CUSTOMER = "customer";
    public static final String ROLE_STORE = "store";
    public static final String ROLE_ADMIN = "admin";

    // Order Status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_SHIPPING = "shipping";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    // Payment Methods
    public static final String PAYMENT_COD = "cod";
    public static final String PAYMENT_ONLINE = "online";

    // Intent Extras
    public static final String EXTRA_STORE_ID = "store_id";
    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_USER_ID = "user_id";

    // Categories
    public static final String[] PRODUCT_CATEGORIES = {
            "Thực phẩm",
            "Đồ uống",
            "Điện tử",
            "Thời trang",
            "Gia dụng",
            "Sức khỏe",
            "Khác"
    };
}
