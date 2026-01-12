package com.example.svg_adr.ui.store;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
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
import com.example.svg_adr.adapter.CategoryAdapter;
import com.example.svg_adr.model.Category;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView rvCategories;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    private CategoryAdapter categoryAdapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String storeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadStoreId();
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rvCategories);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý danh mục");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(this);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCategories);
        swipeRefresh.setColorSchemeResources(R.color.primary);
        fabAdd.setOnClickListener(v -> showCategoryDialog(null));
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
                        loadCategories();
                        break;
                    }
                });
    }

    private void loadCategories() {
        if (storeId == null)
            return;

        showLoading(true);
        db.collection(Constants.COLLECTION_CATEGORIES)
                .whereEqualTo("storeId", storeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    List<Category> categories = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Category category = doc.toObject(Category.class);
                        category.setId(doc.getId());
                        categories.add(category);
                    }

                    // Sort client-side to avoid requiring a composite index
                    categories.sort((c1, c2) -> {
                        long t1 = 0, t2 = 0;
                        if (c1.getCreatedAt() instanceof Long)
                            t1 = (Long) c1.getCreatedAt();
                        else if (c1.getCreatedAt() instanceof com.google.firebase.Timestamp)
                            t1 = ((com.google.firebase.Timestamp) c1.getCreatedAt()).toDate().getTime();

                        if (c2.getCreatedAt() instanceof Long)
                            t2 = (Long) c2.getCreatedAt();
                        else if (c2.getCreatedAt() instanceof com.google.firebase.Timestamp)
                            t2 = ((com.google.firebase.Timestamp) c2.getCreatedAt()).toDate().getTime();

                        return Long.compare(t2, t1); // Descending
                    });

                    categoryAdapter.setCategories(categories);
                    layoutEmpty.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
                    rvCategories.setVisibility(categories.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showCategoryDialog(Category category) {
        boolean isEdit = category != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Sửa danh mục" : "Thêm danh mục mới");

        final EditText input = new EditText(this);
        input.setHint("Tên danh mục");
        if (isEdit)
            input.setText(category.getName());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(40, 0, 40, 0);
        input.setLayoutParams(lp);

        builder.setView(input);

        builder.setPositiveButton(isEdit ? "Cập nhật" : "Thêm", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isEdit) {
                updateCategory(category, name);
            } else {
                addCategory(name);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addCategory(String name) {
        if (storeId == null)
            return;
        showLoading(true);
        Category category = new Category(name, storeId);
        db.collection(Constants.COLLECTION_CATEGORIES)
                .add(category.toMap())
                .addOnSuccessListener(ref -> {
                    showLoading(false);
                    Toast.makeText(this, "Đã thêm danh mục", Toast.LENGTH_SHORT).show();
                    loadCategories();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateCategory(Category category, String newName) {
        showLoading(true);
        db.collection(Constants.COLLECTION_CATEGORIES)
                .document(category.getId())
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    loadCategories();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteCategory(Category category) {
        showLoading(true);
        db.collection(Constants.COLLECTION_CATEGORIES)
                .document(category.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
                    loadCategories();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEdit(Category category) {
        showCategoryDialog(category);
    }

    @Override
    public void onDelete(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Bạn có chắc muốn xóa danh mục " + category.getName() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteCategory(category))
                .setNegativeButton("Hủy", null)
                .show();
    }
}
