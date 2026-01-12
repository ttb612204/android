package com.example.svg_adr.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.svg_adr.R;
import com.example.svg_adr.adapter.StoreAdapter;
import com.example.svg_adr.model.Store;
import com.example.svg_adr.model.User;
import com.example.svg_adr.ui.auth.LoginActivity;
import com.example.svg_adr.utils.CartManager;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements StoreAdapter.OnStoreClickListener, NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private EditText etSearch;
    private RecyclerView rvStores;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabCart;
    private BottomNavigationView bottomNav;

    private StoreAdapter storeAdapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private CartManager cartManager;

    private List<Store> allStores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        cartManager = new CartManager(this);

        initViews();
        setupToolbar();
        setupNavDrawer();
        setupRecyclerView();
        setupListeners();
        loadStores();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        etSearch = findViewById(R.id.etSearch);
        rvStores = findViewById(R.id.rvStores);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabCart = findViewById(R.id.fabCart);
        bottomNav = findViewById(R.id.bottomNav);
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

        View header = navView.getHeaderView(0);
        TextView tvUserName = header.findViewById(R.id.tvUserName);
        TextView tvUserEmail = header.findViewById(R.id.tvUserEmail);

        User user = sessionManager.getUser();
        if (user != null) {
            tvUserName.setText(user.getName());
            tvUserEmail.setText(user.getEmail());
        }
    }

    private void setupRecyclerView() {
        storeAdapter = new StoreAdapter(this, this);
        rvStores.setLayoutManager(new LinearLayoutManager(this));
        rvStores.setAdapter(storeAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadStores);
        swipeRefresh.setColorSchemeResources(R.color.primary);

        fabCart.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStores(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, OrderHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });
    }

    private void loadStores() {
        showLoading(true);
        db.collection(Constants.COLLECTION_STORES)
                .whereEqualTo("isApproved", true)
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

                    storeAdapter.setStores(allStores);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void filterStores(String query) {
        if (query.isEmpty()) {
            storeAdapter.setStores(allStores);
        } else {
            List<Store> filtered = new ArrayList<>();
            for (Store store : allStores) {
                if (store.getName().toLowerCase().contains(query.toLowerCase()) ||
                        store.getDescription().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(store);
                }
            }
            storeAdapter.setStores(filtered);
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (storeAdapter.getItemCount() == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvStores.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvStores.setVisibility(View.VISIBLE);
        }
    }

    private void updateCartBadge() {
        int count = cartManager.getItemCount();
        // Could add badge to FAB here if needed
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already here
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, OrderHistoryActivity.class));
        } else if (id == R.id.nav_favorites) {
            startActivity(new Intent(this, FavoritesActivity.class));
        } else if (id == R.id.nav_cart) {
            startActivity(new Intent(this, CartActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, EditProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        sessionManager.logout();
        cartManager.clearCart();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStoreClick(Store store) {
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra(Constants.EXTRA_STORE_ID, store.getId());
        intent.putExtra("store_name", store.getName());
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
