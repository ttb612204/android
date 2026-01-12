package com.example.svg_adr.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.svg_adr.R;
import com.example.svg_adr.model.CartItem;
import com.example.svg_adr.model.Order;
import com.example.svg_adr.model.User;
import com.example.svg_adr.utils.CartManager;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private TextInputEditText etAddress, etPhone, etNote;
    private RadioGroup rgPayment;
    private RadioButton rbCOD, rbOnline;
    private CheckBox cbSaveAddress;
    private TextView tvItemCount, tvSubtotal, tvTotal;
    private MaterialButton btnPlaceOrder;
    private ProgressBar progressBar;

    private CartManager cartManager;
    private SessionManager sessionManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        cartManager = new CartManager(this);
        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        loadOrderSummary();
        prefillUserData();
        setupListeners();
    }

    private void initViews() {
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        etNote = findViewById(R.id.etNote);
        rgPayment = findViewById(R.id.rgPayment);
        rbCOD = findViewById(R.id.rbCOD);
        rbOnline = findViewById(R.id.rbOnline);
        cbSaveAddress = findViewById(R.id.cbSaveAddress);
        tvItemCount = findViewById(R.id.tvItemCount);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thanh toán");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void prefillUserData() {
        User user = sessionManager.getUser();
        if (user != null) {
            if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                etAddress.setText(user.getAddress());
            }
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                etPhone.setText(user.getPhone());
            }
        }
    }

    private void loadOrderSummary() {
        List<CartItem> items = cartManager.getItems();
        int count = cartManager.getItemCount();
        double total = cartManager.getTotal();

        tvItemCount.setText(String.valueOf(count));
        tvSubtotal.setText(String.format("%,.0f₫", total));
        tvTotal.setText(String.format("%,.0f₫", total));
    }

    private void setupListeners() {
        btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void placeOrder() {
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String paymentMethod = rbCOD.isChecked() ? Constants.PAYMENT_COD : Constants.PAYMENT_ONLINE;
        boolean saveAddress = cbSaveAddress.isChecked();

        // Validation
        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Vui lòng nhập địa chỉ");
            etAddress.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }

        List<CartItem> cartItems = cartManager.getItems();
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Save address if checkbox is checked
        if (saveAddress) {
            saveUserAddress(address, phone);
        }

        // Group items by store
        Map<String, List<CartItem>> itemsByStore = new HashMap<>();
        for (CartItem item : cartItems) {
            String storeId = item.getStoreId();
            if (!itemsByStore.containsKey(storeId)) {
                itemsByStore.put(storeId, new ArrayList<>());
            }
            itemsByStore.get(storeId).add(item);
        }

        User user = sessionManager.getUser();
        int totalOrders = itemsByStore.size();
        int[] completedOrders = { 0 };

        // Create an order for each store
        for (Map.Entry<String, List<CartItem>> entry : itemsByStore.entrySet()) {
            String storeId = entry.getKey();
            List<CartItem> storeItems = entry.getValue();

            Order order = new Order();
            order.setUserId(user != null ? user.getId() : "");
            order.setUserName(user != null ? user.getName() : "");
            order.setUserPhone(phone);
            order.setStoreId(storeId);
            order.setStoreName(storeItems.get(0).getStoreName());
            order.setAddress(address);
            order.setPaymentMethod(paymentMethod);
            order.setNote(note);

            List<Order.OrderItem> orderItems = new ArrayList<>();
            double total = 0;
            for (CartItem cartItem : storeItems) {
                orderItems.add(cartItem.toOrderItem());
                total += cartItem.getSubtotal();
            }
            order.setItems(orderItems);
            order.setTotalAmount(total);

            db.collection(Constants.COLLECTION_ORDERS)
                    .add(order.toMap())
                    .addOnSuccessListener(documentReference -> {
                        completedOrders[0]++;
                        if (completedOrders[0] == totalOrders) {
                            showLoading(false);
                            cartManager.clearCart();
                            showSuccessDialog();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi đặt hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void saveUserAddress(String address, String phone) {
        User user = sessionManager.getUser();
        if (user == null || user.getId() == null)
            return;

        // Update user in Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("address", address);
        updates.put("phone", phone);

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local session
                    user.setAddress(address);
                    user.setPhone(phone);
                    sessionManager.saveUser(user);
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đặt hàng thành công!")
                .setMessage(
                        "Đơn hàng của bạn đã được gửi đến cửa hàng. Bạn có thể theo dõi trạng thái đơn hàng trong mục 'Đơn hàng'.")
                .setPositiveButton("Xem đơn hàng", (dialog, which) -> {
                    Intent intent = new Intent(this, OrderHistoryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Tiếp tục mua sắm", (dialog, which) -> {
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPlaceOrder.setEnabled(!show);
    }
}
