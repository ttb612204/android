package com.example.svg_adr.ui.store;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.svg_adr.R;
import com.example.svg_adr.model.Product;
import com.example.svg_adr.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEditProductActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription, etPrice, etImageUrl;
    private Spinner spinnerCategory;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String storeId, productId;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        storeId = getIntent().getStringExtra(Constants.EXTRA_STORE_ID);
        productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        isEdit = productId != null;

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupCategory();
        setupListeners();

        if (isEdit) {
            // Wait for category loading in setupCategory
        }
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etImageUrl = findViewById(R.id.etImageUrl);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEdit ? "Sửa sản phẩm" : "Thêm sản phẩm");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategory() {
        if (storeId == null)
            return;

        db.collection(Constants.COLLECTION_CATEGORIES)
                .whereEqualTo("storeId", storeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> categories = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        categories.add(doc.getString("name"));
                    }

                    if (categories.isEmpty()) {
                        Toast.makeText(this, "Bạn cần tạo danh mục trước khi thêm sản phẩm", Toast.LENGTH_LONG).show();
                        // Optional: finish() or redirect
                        categories.add("Chưa có danh mục");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, categories);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);

                    // If editing, re-set the selection since adapter might have loaded late
                    if (isEdit) {
                        loadProduct();
                    }
                });
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void loadProduct() {
        showLoading(true);
        db.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);
                    if (doc.exists()) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            etName.setText(product.getName());
                            etDescription.setText(product.getDescription());
                            etPrice.setText(String.valueOf((int) product.getPrice()));
                            etImageUrl.setText(product.getImageUrl());

                            // Set category
                            for (int i = 0; i < spinnerCategory.getCount(); i++) {
                                if (spinnerCategory.getItemAtPosition(i).toString().equals(product.getCategory())) {
                                    spinnerCategory.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Vui lòng nhập tên sản phẩm");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Vui lòng nhập giá");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPrice.setError("Giá không hợp lệ");
            return;
        }

        showLoading(true);

        Product product = new Product(storeId, name, description, price, category);
        product.setImageUrl(imageUrl);
        product.setApproved(false); // Need admin approval

        if (isEdit) {
            product.setId(productId);
            db.collection(Constants.COLLECTION_PRODUCTS)
                    .document(productId)
                    .update(product.toMap())
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Toast.makeText(this, "Đã cập nhật sản phẩm", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            db.collection(Constants.COLLECTION_PRODUCTS)
                    .add(product.toMap())
                    .addOnSuccessListener(docRef -> {
                        showLoading(false);
                        Toast.makeText(this, "Đã thêm sản phẩm. Chờ admin phê duyệt.", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }
}
