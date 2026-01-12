package com.example.svg_adr.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.svg_adr.R;
import com.example.svg_adr.model.Store;
import com.example.svg_adr.model.User;
import com.example.svg_adr.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputEditText etStoreName, etStoreAddress, etStoreDescription;
    private RadioGroup rgAccountType;
    private RadioButton rbCustomer, rbStore;
    private LinearLayout layoutStoreInfo;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etStoreName = findViewById(R.id.etStoreName);
        etStoreAddress = findViewById(R.id.etStoreAddress);
        etStoreDescription = findViewById(R.id.etStoreDescription);
        rgAccountType = findViewById(R.id.rgAccountType);
        rbCustomer = findViewById(R.id.rbCustomer);
        rbStore = findViewById(R.id.rbStore);
        layoutStoreInfo = findViewById(R.id.layoutStoreInfo);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        rgAccountType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStore) {
                layoutStoreInfo.setVisibility(View.VISIBLE);
            } else {
                layoutStoreInfo.setVisibility(View.GONE);
            }
        });

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        boolean isStore = rbStore.isChecked();

        // Validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Vui lòng nhập họ tên");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Validate store info if store account
        String storeName = "", storeAddress = "", storeDescription = "";
        if (isStore) {
            storeName = etStoreName.getText().toString().trim();
            storeAddress = etStoreAddress.getText().toString().trim();
            storeDescription = etStoreDescription.getText().toString().trim();

            if (TextUtils.isEmpty(storeName)) {
                etStoreName.setError("Vui lòng nhập tên cửa hàng");
                etStoreName.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(storeAddress)) {
                etStoreAddress.setError("Vui lòng nhập địa chỉ cửa hàng");
                etStoreAddress.requestFocus();
                return;
            }
        }

        showLoading(true);

        String finalStoreName = storeName;
        String finalStoreAddress = storeAddress;
        String finalStoreDescription = storeDescription;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            createUserInFirestore(
                                    firebaseUser.getUid(),
                                    name,
                                    email,
                                    phone,
                                    isStore,
                                    finalStoreName,
                                    finalStoreAddress,
                                    finalStoreDescription);
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Đăng ký thất bại";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserInFirestore(String userId, String name, String email, String phone,
            boolean isStore, String storeName, String storeAddress,
            String storeDescription) {
        User user = new User(email, name, isStore ? Constants.ROLE_STORE : Constants.ROLE_CUSTOMER);
        user.setId(userId);
        user.setPhone(phone);
        user.setApproved(!isStore); // Customers auto-approved, stores need admin approval

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .set(user.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (isStore) {
                        // Create store document
                        Store store = new Store(userId, storeName, storeDescription, storeAddress, phone);
                        db.collection(Constants.COLLECTION_STORES)
                                .add(store.toMap())
                                .addOnSuccessListener(docRef -> {
                                    showLoading(false);
                                    Toast.makeText(this,
                                            "Đăng ký thành công! Vui lòng chờ admin phê duyệt cửa hàng.",
                                            Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    showLoading(false);
                                    Toast.makeText(this, "Lỗi tạo cửa hàng: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}
