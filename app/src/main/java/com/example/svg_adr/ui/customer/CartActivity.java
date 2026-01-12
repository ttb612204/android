package com.example.svg_adr.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.svg_adr.R;
import com.example.svg_adr.adapter.CartAdapter;
import com.example.svg_adr.model.CartItem;
import com.example.svg_adr.utils.CartManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemListener {

    private RecyclerView rvCartItems;
    private LinearLayout layoutEmpty, layoutCheckout;
    private TextView tvTotal;
    private MaterialButton btnCheckout, btnShopping;

    private CartAdapter cartAdapter;
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = new CartManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void initViews() {
        rvCartItems = findViewById(R.id.rvCartItems);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutCheckout = findViewById(R.id.layoutCheckout);
        tvTotal = findViewById(R.id.tvTotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnShopping = findViewById(R.id.btnShopping);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Giỏ hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, this);
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        btnCheckout.setOnClickListener(v -> {
            startActivity(new Intent(this, CheckoutActivity.class));
        });

        btnShopping.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void loadCart() {
        List<CartItem> items = cartManager.getItems();
        cartAdapter.setItems(items);
        updateUI();
    }

    private void updateUI() {
        List<CartItem> items = cartManager.getItems();
        if (items.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvCartItems.setVisibility(View.GONE);
            layoutCheckout.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.VISIBLE);
            layoutCheckout.setVisibility(View.VISIBLE);

            double total = cartManager.getTotal();
            tvTotal.setText(String.format("%,.0f₫", total));
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        cartManager.updateQuantity(item.getProductId(), newQuantity);
        loadCart();
    }

    @Override
    public void onItemDelete(CartItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc muốn xóa " + item.getProductName() + " khỏi giỏ hàng?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    cartManager.removeItem(item.getProductId());
                    loadCart();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
