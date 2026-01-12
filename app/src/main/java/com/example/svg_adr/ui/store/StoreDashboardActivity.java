package com.example.svg_adr.ui.store;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.svg_adr.R;
import com.example.svg_adr.adapter.OrderAdapter;
import com.example.svg_adr.model.Order;
import com.example.svg_adr.model.Store;
import com.example.svg_adr.model.User;
import com.example.svg_adr.ui.auth.LoginActivity;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StoreDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OrderAdapter.OnOrderClickListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private TextView tvPendingOrders, tvRevenue, tvTotalOrders, tvNoOrders;
    private TextView tvCompletedOrders, tvCancelledOrders;
    private CardView cardOrders, cardProducts, cardCategories;
    private RecyclerView rvRecentOrders;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private OrderAdapter orderAdapter;
    private String storeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_dashboard);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupNavDrawer();
        setupRecyclerView();
        setupListeners();
        loadStoreData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        tvPendingOrders = findViewById(R.id.tvPendingOrders);
        tvRevenue = findViewById(R.id.tvRevenue);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvCancelledOrders = findViewById(R.id.tvCancelledOrders);
        tvNoOrders = findViewById(R.id.tvNoOrders);
        cardOrders = findViewById(R.id.cardOrders);
        cardProducts = findViewById(R.id.cardProducts);
        cardCategories = findViewById(R.id.cardCategories);
        rvRecentOrders = findViewById(R.id.rvRecentOrders);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavDrawer() {
        navView.setNavigationItemSelectedListener(this);

        // Update header
        View header = navView.getHeaderView(0);
        TextView tvStoreName = header.findViewById(R.id.tvStoreName);
        TextView tvStoreEmail = header.findViewById(R.id.tvStoreEmail);

        User user = sessionManager.getUser();
        if (user != null) {
            tvStoreName.setText(user.getName());
            tvStoreEmail.setText(user.getEmail());
        }
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(this, this, true); // true = store view
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(this));
        rvRecentOrders.setAdapter(orderAdapter);
    }

    private void setupListeners() {
        cardOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, StoreOrdersActivity.class));
        });

        cardProducts.setOnClickListener(v -> {
            startActivity(new Intent(this, ProductManagementActivity.class));
        });

        cardCategories.setOnClickListener(v -> {
            startActivity(new Intent(this, CategoryManagementActivity.class));
        });
    }

    private void loadStoreData() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        db.collection(Constants.COLLECTION_STORES)
                .whereEqualTo("ownerId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        storeId = doc.getId();
                        Store store = doc.toObject(Store.class);
                        if (store != null) {
                            tvRevenue.setText(String.format("%,.0f₫", store.getTotalRevenue()));
                            tvTotalOrders.setText(String.valueOf(store.getOrderCount()));
                        }
                        loadDashboardData();
                        break;
                    }
                });
    }

    private void loadDashboardData() {
        if (storeId == null)
            return;

        // Load pending orders count and recent orders
        db.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("storeId", storeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Order> orders = new ArrayList<>();
                    int pendingCount = 0;
                    int completedCount = 0;
                    int cancelledCount = 0;
                    int totalCount = queryDocumentSnapshots.size();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = parseOrder(doc);
                        orders.add(order);

                        String status = order.getStatus();
                        if (Order.STATUS_PENDING.equals(status)) {
                            pendingCount++;
                        } else if (Order.STATUS_COMPLETED.equals(status)) {
                            completedCount++;
                        } else if (Order.STATUS_CANCELLED.equals(status)) {
                            cancelledCount++;
                        }
                    }

                    tvPendingOrders.setText(String.valueOf(pendingCount));
                    tvTotalOrders.setText(String.valueOf(totalCount));
                    tvCompletedOrders.setText(String.valueOf(completedCount));
                    tvCancelledOrders.setText(String.valueOf(cancelledCount));

                    // Sort client-side by date descending
                    Collections.sort(orders, new java.util.Comparator<Order>() {
                        @Override
                        public int compare(Order o1, Order o2) {
                            return Long.compare(o2.getCreatedAt(), o1.getCreatedAt());
                        }
                    });

                    // Show only first 5 in the "Recent" list
                    List<Order> recentOrders = orders.size() > 5 ? orders.subList(0, 5) : orders;
                    orderAdapter.setOrders(recentOrders);
                    tvNoOrders.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private Order parseOrder(QueryDocumentSnapshot doc) {
        Order order = new Order();
        order.setId(doc.getId());
        order.setUserId(doc.getString("userId"));
        order.setUserName(doc.getString("userName"));
        order.setUserPhone(doc.getString("userPhone"));
        order.setStoreId(doc.getString("storeId"));
        order.setStoreName(doc.getString("storeName"));
        order.setStatus(doc.getString("status"));

        Double total = doc.getDouble("totalAmount");
        order.setTotalAmount(total != null ? total : 0);

        Long createdAt = doc.getLong("createdAt");
        order.setCreatedAt(createdAt != null ? createdAt : 0);

        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) doc.get("items");
        if (itemMaps != null) {
            List<Order.OrderItem> items = new ArrayList<>();
            for (Map<String, Object> itemMap : itemMaps) {
                Order.OrderItem item = new Order.OrderItem();
                item.setProductName((String) itemMap.get("productName"));
                items.add(item);
            }
            order.setItems(items);
        }

        return order;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, StoreOrdersActivity.class));
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(this, ProductManagementActivity.class));
        } else if (id == R.id.nav_edit_store) {
            startActivity(new Intent(this, EditStoreActivity.class));
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, StoreOrdersActivity.class);
        intent.putExtra(Constants.EXTRA_ORDER_ID, order.getId());
        startActivity(intent);
    }
}
