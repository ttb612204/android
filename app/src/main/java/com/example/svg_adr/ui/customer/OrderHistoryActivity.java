package com.example.svg_adr.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

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

public class OrderHistoryActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {

    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private OrderAdapter orderAdapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private List<Order> allOrders = new ArrayList<>();
    private String currentFilter = null; // null = all

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupListeners();
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
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
            getSupportActionBar().setTitle("Đơn hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ xác nhận"));
        tabLayout.addTab(tabLayout.newTab().setText("Đang giao"));
        tabLayout.addTab(tabLayout.newTab().setText("Hoàn thành"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã hủy"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1:
                        currentFilter = Order.STATUS_PENDING;
                        break;
                    case 2:
                        currentFilter = Order.STATUS_SHIPPING;
                        break;
                    case 3:
                        currentFilter = Order.STATUS_COMPLETED;
                        break;
                    case 4:
                        currentFilter = Order.STATUS_CANCELLED;
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
        orderAdapter = new OrderAdapter(this, this);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadOrders);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }

    private void loadOrders() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        db.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = parseOrder(doc);
                        if (order != null) {
                            allOrders.add(order);
                        }
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
        order.setPaymentMethod(doc.getString("paymentMethod"));
        order.setNote(doc.getString("note"));
        order.setStatus(doc.getString("status"));

        Double totalAmount = doc.getDouble("totalAmount");
        order.setTotalAmount(totalAmount != null ? totalAmount : 0);

        Long createdAt = doc.getLong("createdAt");
        order.setCreatedAt(createdAt != null ? createdAt : 0);

        // Parse items
        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) doc.get("items");
        if (itemMaps != null) {
            List<Order.OrderItem> items = new ArrayList<>();
            for (Map<String, Object> itemMap : itemMaps) {
                Order.OrderItem item = new Order.OrderItem();
                item.setProductId((String) itemMap.get("productId"));
                item.setProductName((String) itemMap.get("productName"));
                item.setProductImage((String) itemMap.get("productImage"));

                Object priceObj = itemMap.get("price");
                item.setPrice(priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0);

                Object qtyObj = itemMap.get("quantity");
                item.setQuantity(qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0);

                items.add(item);
            }
            order.setItems(items);
        }

        return order;
    }

    private void filterOrders() {
        List<Order> filtered;
        if (currentFilter == null) {
            filtered = new ArrayList<>(allOrders);
        } else {
            filtered = new ArrayList<>();
            for (Order order : allOrders) {
                if (currentFilter.equals(order.getStatus())) {
                    filtered.add(order);
                }
            }
        }
        orderAdapter.setOrders(filtered);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (orderAdapter.getItemCount() == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(Constants.EXTRA_ORDER_ID, order.getId());
        startActivity(intent);
    }
}
