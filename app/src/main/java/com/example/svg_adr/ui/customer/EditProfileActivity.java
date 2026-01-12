package com.example.svg_adr.ui.customer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.svg_adr.R;
import com.example.svg_adr.model.User;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private FloatingActionButton fabEditAvatar;
    private TextInputEditText etName, etPhone, etAddress;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        currentUser = sessionManager.getUser();

        if (currentUser == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        fabEditAvatar = findViewById(R.id.fabEditAvatar);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chỉnh sửa hồ sơ");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        etName.setText(currentUser.getName());
        etPhone.setText(currentUser.getPhone());
        etAddress.setText(currentUser.getAddress());

        if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getAvatarUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveProfile());

        fabEditAvatar.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng tải ảnh lên đang được cập nhật", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Vui lòng nhập tên");
            return;
        }

        showLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        db.collection(Constants.COLLECTION_USERS)
                .document(currentUser.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    // Update current user object and session
                    currentUser.setName(name);
                    currentUser.setPhone(phone);
                    currentUser.setAddress(address);
                    sessionManager.saveUser(currentUser);

                    Toast.makeText(this, "Đã cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }
}
