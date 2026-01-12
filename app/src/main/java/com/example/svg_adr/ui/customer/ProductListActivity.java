package com.example.svg_adr.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.svg_adr.R;
import com.example.svg_adr.adapter.ProductAdapter;
import com.example.svg_adr.model.Favorite;
import com.example.svg_adr.model.Product;
import com.example.svg_adr.model.Store;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.example.svg_adr.utils.SpaceItemDecoration;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private RecyclerView rvProducts;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private ChipGroup chipGroupCategories;
    private FloatingActionButton fabCart;
    private EditText etSearch;

    private FirebaseFirestore db;
    private ProductAdapter productAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private String storeId;
    private String selectedCategory = null;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        storeId = getIntent().getStringExtra(Constants.EXTRA_STORE_ID);
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupCategories();
        setupListeners();
        loadProducts();
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rvProducts);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        fabCart = findViewById(R.id.fabCart);
        etSearch = findViewById(R.id.etSearch);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sản phẩm");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(this, this);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.addItemDecoration(new SpaceItemDecoration(8));
        rvProducts.setAdapter(productAdapter);
    }

    private void setupCategories() {
        // Clear previous chips if any
        chipGroupCategories.removeAllViews();

        // Add "Tất cả" chip
        Chip chipAll = new Chip(this);
        chipAll.setText("Tất cả");
        chipAll.setCheckable(true);
        chipAll.setChecked(true);
        chipGroupCategories.addView(chipAll);

        if (storeId != null) {
            // Fetch dynamic categories for this store
            db.collection(Constants.COLLECTION_CATEGORIES)
                    .whereEqualTo("storeId", storeId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String category = doc.getString("name");
                            addCategoryChip(category);
                        }
                    });
        } else {
            // Add default category chips for global browsing
            for (String category : Constants.PRODUCT_CATEGORIES) {
                addCategoryChip(category);
            }
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = null;
            } else {
                Chip selectedChip = findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    String text = selectedChip.getText().toString();
                    selectedCategory = text.equals("Tất cả") ? null : text;
                }
            }
            applyFilters();
        });
    }

    private void addCategoryChip(String category) {
        Chip chip = new Chip(this);
        chip.setText(category);
        chip.setCheckable(true);
        chipGroupCategories.addView(chip);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadProducts);
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
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadProducts() {
        showLoading(true);
        Query query = db.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("isApproved", true);

        if (storeId != null) {
            query = query.whereEqualTo("storeId", storeId);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            allProducts.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Product product = doc.toObject(Product.class);
                product.setId(doc.getId());
                allProducts.add(product);
            }
            applyFilters();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void applyFilters() {
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();
        List<Product> filteredList = new ArrayList<>();

        for (Product product : allProducts) {
            boolean matchesCategory = (selectedCategory == null || product.getCategory().equals(selectedCategory));
            boolean matchesSearch = (searchQuery.isEmpty() || product.getName().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filteredList.add(product);
            }
        }

        productAdapter.setProducts(filteredList);
        layoutEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (!show) {
            swipeRefresh.setRefreshing(false);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(Constants.EXTRA_PRODUCT_ID, product.getId());
        intent.putExtra(Constants.EXTRA_STORE_ID, product.getStoreId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Product product) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already favorited
        db.collection(Constants.COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        addToFavorites(userId, product);
                    } else {
                        // Remove from favorites
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            db.collection(Constants.COLLECTION_FAVORITES).document(doc.getId()).delete();
                        }
                        Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addToFavorites(String userId, Product product) {
        // We need store info too, let's fetch it or just use a partial store if we
        // don't have it
        db.collection(Constants.COLLECTION_STORES).document(product.getStoreId()).get()
                .addOnSuccessListener(doc -> {
                    Store store = doc.toObject(Store.class);
                    if (store != null) {
                        store.setId(doc.getId());
                        Favorite favorite = new Favorite(userId, product, store);
                        db.collection(Constants.COLLECTION_FAVORITES).add(favorite.toMap())
                                .addOnSuccessListener(ref -> Toast
                                        .makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show());
                    }
                });
    }
}
