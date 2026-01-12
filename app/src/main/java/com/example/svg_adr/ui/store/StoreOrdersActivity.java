package com.example.svg_adr.ui.store;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.svg_adr.R;
import com.example.svg_adr.adapter.OrderAdapter;
import com.example.svg_adr.model.Order;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoreOrdersActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {

    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private OrderAdapter orderAdapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String storeId;

    private List<Order> allOrders = new ArrayList<>();
    private String currentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history); // Reuse layout

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupListeners();
        loadStoreId();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        rvOrders = findViewById(R.id.rvOrders);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý đơn hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ xác nhận"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã xác nhận"));
        tabLayout.addTab(tabLayout.newTab().setText("Đang giao"));
        tabLayout.addTab(tabLayout.newTab().setText("Hoàn thành"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1:
                        currentFilter = Order.STATUS_PENDING;
                        break;
                    case 2:
                        currentFilter = Order.STATUS_CONFIRMED;
                        break;
                    case 3:
                        currentFilter = Order.STATUS_SHIPPING;
                        break;
                    case 4:
                        currentFilter = Order.STATUS_COMPLETED;
                        break;
                    default:
                        currentFilter = null;
                }
                filterOrders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(this, this, true);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadOrders);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }

    private void loadStoreId() {
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
                        loadOrders();
                        break;
                    }
                });
    }

    private void loadOrders() {
        if (storeId == null)
            return;

        showLoading(true);
        db.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("storeId", storeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = parseOrder(doc);
                        allOrders.add(order);
                    }
                    filterOrders();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        order.setAddress(doc.getString("address"));
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
                item.setProductId((String) itemMap.get("productId"));
                item.setProductName((String) itemMap.get("productName"));
                Object qtyObj = itemMap.get("quantity");
                item.setQuantity(qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1);
                items.add(item);
            }
            order.setItems(items);
        }

        return order;
    }

    private void filterOrders() {
        List<Order> filtered = currentFilter == null ? new ArrayList<>(allOrders) : new ArrayList<>();

        if (currentFilter != null) {
            for (Order order : allOrders) {
                if (currentFilter.equals(order.getStatus())) {
                    filtered.add(order);
                }
            }
        }

        orderAdapter.setOrders(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvOrders.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOrderClick(Order order) {
        showOrderActionDialog(order);
    }

    private void showOrderActionDialog(Order order) {
        String[] options;
        if (order.canConfirm()) {
            options = new String[] { "Xác nhận đơn hàng", "Từ chối đơn hàng" };
        } else if (order.canShip()) {
            options = new String[] { "Bắt đầu giao hàng" };
        } else if (order.canComplete()) {
            options = new String[] { "Hoàn thành đơn hàng" };
        } else {
            Toast.makeText(this, "Đơn hàng: " + order.getStatusText(), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cập nhật đơn hàng")
                .setItems(options, (dialog, which) -> {
                    if (order.canConfirm()) {
                        if (which == 0)
                            updateOrderStatus(order, Order.STATUS_CONFIRMED);
                        else
                            updateOrderStatus(order, Order.STATUS_CANCELLED);
                    } else if (order.canShip()) {
                        updateOrderStatus(order, Order.STATUS_SHIPPING);
                    } else if (order.canComplete()) {
                        updateOrderStatus(order, Order.STATUS_COMPLETED);
                    }
                })
                .show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        showLoading(true);
        db.collection(Constants.COLLECTION_ORDERS)
                .document(order.getId())
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();

                    // Update revenue and soldCount if completed
                    if (Order.STATUS_COMPLETED.equals(newStatus)) {
                        updateStoreRevenue(order.getTotalAmount());
                        updateProductSoldCount(order);
                    }

                    loadOrders();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateStoreRevenue(double amount) {
        if (storeId == null)
            return;

        db.collection(Constants.COLLECTION_STORES)
                .document(storeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double currentRevenue = doc.getDouble("totalRevenue");
                        double newRevenue = (currentRevenue != null ? currentRevenue : 0) + amount;

                        Integer currentCount = doc.getLong("orderCount") != null ? doc.getLong("orderCount").intValue()
                                : 0;

                        db.collection(Constants.COLLECTION_STORES)
                                .document(storeId)
                                .update("totalRevenue", newRevenue, "orderCount", currentCount + 1);
                    }
                });
    }

    private void updateProductSoldCount(Order order) {
        List<Order.OrderItem> items = order.getItems();
        if (items == null)
            return;

        for (Order.OrderItem item : items) {
            if (item.getProductId() == null)
                continue;

            db.collection(Constants.COLLECTION_PRODUCTS)
                    .document(item.getProductId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Long currentSold = doc.getLong("soldCount");
                            int newSold = (currentSold != null ? currentSold.intValue() : 0) + item.getQuantity();

                            db.collection(Constants.COLLECTION_PRODUCTS)
                                    .document(item.getProductId())
                                    .update("soldCount", newSold);
                        }
                    });
        }
    }
}
