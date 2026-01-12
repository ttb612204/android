package com.example.svg_adr.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.svg_adr.R;
import com.example.svg_adr.model.Product;
import com.example.svg_adr.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductApprovalActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView rvProducts;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private ProductApprovalAdapter adapter;
    private FirebaseFirestore db;

    private List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_approval);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupListeners();
        loadProducts();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        rvProducts = findViewById(R.id.rvProducts);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Phê duyệt sản phẩm");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Chờ duyệt"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã duyệt"));
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterProducts(tab.getPosition());
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
        adapter = new ProductApprovalAdapter();
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadProducts);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }

    private void loadProducts() {
        showLoading(true);
        db.collection(Constants.COLLECTION_PRODUCTS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    allProducts.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = doc.toObject(Product.class);
                        product.setId(doc.getId());
                        allProducts.add(product);
                    }

                    int selectedTab = tabLayout.getSelectedTabPosition();
                    filterProducts(selectedTab);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void filterProducts(int tabPosition) {
        List<Product> filtered = new ArrayList<>();

        for (Product product : allProducts) {
            boolean match = false;
            switch (tabPosition) {
                case 0: // Pending
                    match = !product.isApproved();
                    break;
                case 1: // Approved
                    match = product.isApproved();
                    break;
                case 2: // All
                    match = true;
                    break;
            }
            if (match)
                filtered.add(product);
        }

        adapter.setProducts(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvProducts.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showApprovalDialog(Product product) {
        String[] options;
        if (product.isApproved()) {
            options = new String[] { "Hủy phê duyệt" };
        } else {
            options = new String[] { "Phê duyệt", "Từ chối" };
        }

        new AlertDialog.Builder(this)
                .setTitle(product.getName())
                .setItems(options, (dialog, which) -> {
                    if (product.isApproved()) {
                        if (which == 0)
                            updateApproval(product, false);
                    } else {
                        if (which == 0)
                            updateApproval(product, true);
                        else if (which == 1)
                            deleteProduct(product);
                    }
                })
                .show();
    }

    private void updateApproval(Product product, boolean approved) {
        showLoading(true);
        db.collection(Constants.COLLECTION_PRODUCTS)
                .document(product.getId())
                .update("isApproved", approved)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, approved ? "Đã phê duyệt" : "Đã hủy phê duyệt",
                            Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận từ chối")
                .setMessage("Bạn có chắc muốn từ chối sản phẩm " + product.getName() + "?")
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    showLoading(true);
                    db.collection(Constants.COLLECTION_PRODUCTS)
                            .document(product.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã từ chối", Toast.LENGTH_SHORT).show();
                                loadProducts();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Inner Adapter class
    private class ProductApprovalAdapter extends RecyclerView.Adapter<ProductApprovalAdapter.ViewHolder> {
        private List<Product> products = new ArrayList<>();

        public void setProducts(List<Product> products) {
            this.products = products;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_approval, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = products.get(position);

            holder.tvName.setText(product.getName());
            holder.tvPrice.setText(product.getFormattedPrice());
            holder.tvCategory.setText(product.getCategory());
            holder.tvStatus.setText(product.isApproved() ? "Đã duyệt" : "Chờ duyệt");
            holder.tvStatus.setTextColor(product.isApproved() ? getColor(R.color.success) : getColor(R.color.warning));

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(ProductApprovalActivity.this)
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.ic_store)
                        .into(holder.ivProduct);
            }

            holder.btnApprove.setVisibility(product.isApproved() ? View.GONE : View.VISIBLE);
            holder.btnReject.setVisibility(product.isApproved() ? View.GONE : View.VISIBLE);

            holder.btnApprove.setOnClickListener(v -> updateApproval(product, true));
            holder.btnReject.setOnClickListener(v -> deleteProduct(product));
            holder.itemView.setOnClickListener(v -> showApprovalDialog(product));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProduct;
            TextView tvName, tvPrice, tvCategory, tvStatus;
            MaterialButton btnApprove, btnReject;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivProduct = itemView.findViewById(R.id.ivProduct);
                tvName = itemView.findViewById(R.id.tvName);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}
