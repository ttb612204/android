package com.example.svg_adr.ui.admin;

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
import com.example.svg_adr.adapter.StoreAdapter;
import com.example.svg_adr.model.Store;
import com.example.svg_adr.utils.Constants;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StoreApprovalActivity extends AppCompatActivity implements StoreAdapter.OnStoreClickListener {

    private TabLayout tabLayout;
    private RecyclerView rvStores;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private StoreAdapter storeAdapter;
    private FirebaseFirestore db;

    private List<Store> allStores = new ArrayList<>();
    private boolean showPendingOnly = false;
    private String filterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_approval);

        showPendingOnly = getIntent().getBooleanExtra("pending_only", false);
        filterType = getIntent().getStringExtra("type");

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupListeners();
        loadStores();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        rvStores = findViewById(R.id.rvStores);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String title = "products".equals(filterType) ? "Phê duyệt sản phẩm" : "Phê duyệt cửa hàng";
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Chờ duyệt"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã duyệt"));
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));

        if (showPendingOnly) {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterStores(tab.getPosition());
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
        storeAdapter = new StoreAdapter(this, this);
        rvStores.setLayoutManager(new LinearLayoutManager(this));
        rvStores.setAdapter(storeAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadStores);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }

    private void loadStores() {
        showLoading(true);
        db.collection(Constants.COLLECTION_STORES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    allStores.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Store store = doc.toObject(Store.class);
                        store.setId(doc.getId());
                        allStores.add(store);
                    }

                    int selectedTab = tabLayout.getSelectedTabPosition();
                    filterStores(selectedTab);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void filterStores(int tabPosition) {
        List<Store> filtered = new ArrayList<>();

        for (Store store : allStores) {
            boolean match = false;
            switch (tabPosition) {
                case 0: // Pending
                    match = !store.isApproved();
                    break;
                case 1: // Approved
                    match = store.isApproved();
                    break;
                case 2: // All
                    match = true;
                    break;
            }
            if (match)
                filtered.add(store);
        }

        storeAdapter.setStores(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvStores.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStoreClick(Store store) {
        showApprovalDialog(store);
    }

    private void showApprovalDialog(Store store) {
        String[] options;
        if (store.isApproved()) {
            options = new String[] { "Hủy phê duyệt", "Xem chi tiết" };
        } else {
            options = new String[] { "Phê duyệt", "Từ chối", "Xem chi tiết" };
        }

        new AlertDialog.Builder(this)
                .setTitle(store.getName())
                .setItems(options, (dialog, which) -> {
                    if (store.isApproved()) {
                        if (which == 0)
                            updateApproval(store, false);
                    } else {
                        if (which == 0)
                            updateApproval(store, true);
                        else if (which == 1)
                            deleteStore(store);
                    }
                })
                .show();
    }

    private void updateApproval(Store store, boolean approved) {
        showLoading(true);
        // Update store approval
        db.collection(Constants.COLLECTION_STORES)
                .document(store.getId())
                .update("isApproved", approved)
                .addOnSuccessListener(aVoid -> {
                    // Also update the store owner's user account approval
                    if (store.getOwnerId() != null) {
                        db.collection(Constants.COLLECTION_USERS)
                                .document(store.getOwnerId())
                                .update("isApproved", approved)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this,
                                            approved ? "Đã phê duyệt cửa hàng và tài khoản" : "Đã hủy phê duyệt",
                                            Toast.LENGTH_SHORT).show();
                                    loadStores();
                                })
                                .addOnFailureListener(e -> {
                                    // Store approved but user update failed
                                    Toast.makeText(this, approved ? "Đã phê duyệt cửa hàng" : "Đã hủy phê duyệt",
                                            Toast.LENGTH_SHORT).show();
                                    loadStores();
                                });
                    } else {
                        Toast.makeText(this, approved ? "Đã phê duyệt" : "Đã hủy phê duyệt",
                                Toast.LENGTH_SHORT).show();
                        loadStores();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteStore(Store store) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận từ chối")
                .setMessage("Bạn có chắc muốn từ chối cửa hàng " + store.getName() + "?")
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    showLoading(true);
                    db.collection(Constants.COLLECTION_STORES)
                            .document(store.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã từ chối", Toast.LENGTH_SHORT).show();
                                loadStores();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
