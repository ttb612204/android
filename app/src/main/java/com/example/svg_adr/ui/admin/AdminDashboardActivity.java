package com.example.svg_adr.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.svg_adr.R;
import com.example.svg_adr.model.User;
import com.example.svg_adr.ui.auth.LoginActivity;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private TextView tvUserCount, tvStoreCount, tvOrderCount, tvTotalRevenue;
    private TextView tvPendingStores, tvPendingProducts;
    private CardView cardPendingStores, cardPendingProducts, cardUsers, cardStores;

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupNavDrawer();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvStoreCount = findViewById(R.id.tvStoreCount);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvPendingStores = findViewById(R.id.tvPendingStores);
        tvPendingProducts = findViewById(R.id.tvPendingProducts);
        cardPendingStores = findViewById(R.id.cardPendingStores);
        cardPendingProducts = findViewById(R.id.cardPendingProducts);
        cardUsers = findViewById(R.id.cardUsers);
        cardStores = findViewById(R.id.cardStores);
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
        TextView tvAdminName = header.findViewById(R.id.tvAdminName);
        TextView tvAdminEmail = header.findViewById(R.id.tvAdminEmail);

        User user = sessionManager.getUser();
        if (user != null) {
            tvAdminName.setText(user.getName());
            tvAdminEmail.setText(user.getEmail());
        }
    }

    private void setupListeners() {
        cardPendingStores.setOnClickListener(v -> {
            Intent intent = new Intent(this, StoreApprovalActivity.class);
            intent.putExtra("pending_only", true);
            startActivity(intent);
        });

        cardPendingProducts.setOnClickListener(v -> {
            startActivity(new Intent(this, ProductApprovalActivity.class));
        });

        cardUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, UserManagementActivity.class));
        });

        cardStores.setOnClickListener(v -> {
            startActivity(new Intent(this, StoreApprovalActivity.class));
        });
    }

    private void loadStats() {
        // User count (exclude admins)
        db.collection(Constants.COLLECTION_USERS)
                .whereNotEqualTo("role", Constants.ROLE_ADMIN)
                .get()
                .addOnSuccessListener(query -> tvUserCount.setText(String.valueOf(query.size())));

        // Store count (approved)
        db.collection(Constants.COLLECTION_STORES)
                .whereEqualTo("isApproved", true)
                .get()
                .addOnSuccessListener(query -> tvStoreCount.setText(String.valueOf(query.size())));

        // Order count
        db.collection(Constants.COLLECTION_ORDERS)
                .get()
                .addOnSuccessListener(query -> tvOrderCount.setText(String.valueOf(query.size())));

        // Pending stores
        db.collection(Constants.COLLECTION_STORES)
                .whereEqualTo("isApproved", false)
                .get()
                .addOnSuccessListener(query -> tvPendingStores.setText(query.size() + " chờ duyệt"));

        // Pending products
        db.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("isApproved", false)
                .get()
                .addOnSuccessListener(query -> tvPendingProducts.setText(query.size() + " chờ duyệt"));

        // Total system revenue
        db.collection(Constants.COLLECTION_STORES)
                .get()
                .addOnSuccessListener(query -> {
                    double total = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                        Double rev = doc.getDouble("totalRevenue");
                        if (rev != null)
                            total += rev;
                    }
                    tvTotalRevenue.setText(String.format("%,.0f₫", total));
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_users) {
            startActivity(new Intent(this, UserManagementActivity.class));
        } else if (id == R.id.nav_stores) {
            startActivity(new Intent(this, StoreApprovalActivity.class));
        } else if (id == R.id.nav_products) {
            startActivity(new Intent(this, ProductApprovalActivity.class));
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
}
