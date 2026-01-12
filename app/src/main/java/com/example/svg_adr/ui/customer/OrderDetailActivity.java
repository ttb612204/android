package com.example.svg_adr.ui.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.svg_adr.R;
import com.example.svg_adr.model.Order;
import com.example.svg_adr.model.Review;
import com.example.svg_adr.model.User;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView tvOrderId, tvStatus, tvStoreName, tvDate, tvAddress, tvPhone, tvPayment, tvNote, tvTotal;
    private RecyclerView rvItems;
    private LinearLayout layoutCancel, layoutRating;
    private MaterialButton btnCancel, btnRate;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String orderId;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getStringExtra(Constants.EXTRA_ORDER_ID);
        if (orderId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        loadOrder();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvStoreName = findViewById(R.id.tvStoreName);
        tvDate = findViewById(R.id.tvDate);
        tvAddress = findViewById(R.id.tvAddress);
        tvPhone = findViewById(R.id.tvPhone);
        tvPayment = findViewById(R.id.tvPayment);
        tvNote = findViewById(R.id.tvNote);
        tvTotal = findViewById(R.id.tvTotal);
        rvItems = findViewById(R.id.rvItems);
        layoutCancel = findViewById(R.id.layoutCancel);
        layoutRating = findViewById(R.id.layoutRating);
        btnCancel = findViewById(R.id.btnCancel);
        btnRate = findViewById(R.id.btnRate);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chi tiết đơn hàng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadOrder() {
        showLoading(true);
        db.collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        currentOrder = parseOrder(documentSnapshot.getId(), documentSnapshot.getData());
                        displayOrder();
                    } else {
                        Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Order parseOrder(String id, Map<String, Object> data) {
        Order order = new Order();
        order.setId(id);
        order.setUserId((String) data.get("userId"));
        order.setUserName((String) data.get("userName"));
        order.setUserPhone((String) data.get("userPhone"));
        order.setStoreId((String) data.get("storeId"));
        order.setStoreName((String) data.get("storeName"));
        order.setAddress((String) data.get("address"));
        order.setPaymentMethod((String) data.get("paymentMethod"));
        order.setNote((String) data.get("note"));
        order.setStatus((String) data.get("status"));

        Object totalObj = data.get("totalAmount");
        order.setTotalAmount(totalObj instanceof Number ? ((Number) totalObj).doubleValue() : 0);

        Object createdAtObj = data.get("createdAt");
        order.setCreatedAt(createdAtObj instanceof Number ? ((Number) createdAtObj).longValue() : 0);

        // Check if order has been rated
        Object isRatedObj = data.get("isRated");
        order.setRated(isRatedObj instanceof Boolean ? (Boolean) isRatedObj : false);

        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) data.get("items");
        if (itemMaps != null) {
            List<Order.OrderItem> items = new ArrayList<>();
            for (Map<String, Object> itemMap : itemMaps) {
                Order.OrderItem item = new Order.OrderItem();
                item.setProductId((String) itemMap.get("productId"));
                item.setProductName((String) itemMap.get("productName"));
                item.setProductImage((String) itemMap.get("productImage"));

                Object priceObj = itemMap.get("price");
                item.setPrice(priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0);

                Object qtyObj = itemMap.get("quantity");
                item.setQuantity(qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0);

                items.add(item);
            }
            order.setItems(items);
        }

        return order;
    }

    private void displayOrder() {
        tvOrderId.setText("Mã đơn: #" + orderId.substring(0, Math.min(8, orderId.length())).toUpperCase());
        tvStatus.setText(currentOrder.getStatusText());
        tvStoreName.setText(currentOrder.getStoreName());
        tvDate.setText(currentOrder.getFormattedDate());
        tvAddress.setText(currentOrder.getAddress());
        tvPhone.setText(currentOrder.getUserPhone());
        tvPayment.setText(Constants.PAYMENT_COD.equals(currentOrder.getPaymentMethod()) ? "Thanh toán khi nhận hàng"
                : "Thanh toán online");
        tvNote.setText(currentOrder.getNote() != null && !currentOrder.getNote().isEmpty() ? currentOrder.getNote()
                : "Không có ghi chú");
        tvTotal.setText(currentOrder.getFormattedTotal());

        // Items list
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(new OrderItemAdapter(currentOrder.getItems()));

        // Cancel button
        if (currentOrder.canCancel()) {
            layoutCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> confirmCancel());
        } else {
            layoutCancel.setVisibility(View.GONE);
        }

        // Rating button - only show for completed orders that haven't been rated
        if (Order.STATUS_COMPLETED.equals(currentOrder.getStatus()) && !currentOrder.isRated()) {
            if (layoutRating != null) {
                layoutRating.setVisibility(View.VISIBLE);
                btnRate.setOnClickListener(v -> showRatingDialog());
            }
        } else {
            if (layoutRating != null) {
                layoutRating.setVisibility(View.GONE);
            }
        }
    }

    private void showRatingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        TextInputEditText etComment = dialogView.findViewById(R.id.etComment);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";
            submitReview(rating, comment);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReview(float rating, String comment) {
        User user = sessionManager.getUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Create reviews for each product in order
        List<Order.OrderItem> items = currentOrder.getItems();
        final int[] completed = { 0 };

        for (Order.OrderItem item : items) {
            Review review = new Review(
                    user.getId(),
                    user.getName(),
                    item.getProductId(),
                    currentOrder.getStoreId(),
                    orderId,
                    rating,
                    comment);

            db.collection(Constants.COLLECTION_REVIEWS)
                    .add(review.toMap())
                    .addOnSuccessListener(docRef -> {
                        // Update product rating
                        updateProductRating(item.getProductId(), rating);

                        completed[0]++;
                        if (completed[0] == items.size()) {
                            // Mark order as rated
                            db.collection(Constants.COLLECTION_ORDERS)
                                    .document(orderId)
                                    .update("isRated", true)
                                    .addOnSuccessListener(aVoid -> {
                                        showLoading(false);
                                        Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                                        if (layoutRating != null) {
                                            layoutRating.setVisibility(View.GONE);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void updateProductRating(String productId, float newRating) {
        db.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double currentRating = doc.getDouble("rating");
                        Long ratingCount = doc.getLong("ratingCount");

                        double oldRating = currentRating != null ? currentRating : 0;
                        int count = ratingCount != null ? ratingCount.intValue() : 0;

                        // Calculate new average
                        double avgRating = ((oldRating * count) + newRating) / (count + 1);

                        db.collection(Constants.COLLECTION_PRODUCTS)
                                .document(productId)
                                .update("rating", avgRating, "ratingCount", count + 1);
                    }
                });
    }

    private void confirmCancel() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrder() {
        showLoading(true);
        db.collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .update("status", Order.STATUS_CANCELLED)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Simple adapter for order items
    private class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder> {
        private List<Order.OrderItem> items;

        OrderItemAdapter(List<Order.OrderItem> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @Override
        public ItemViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_order_product, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            Order.OrderItem item = items.get(position);
            holder.tvName.setText(item.getProductName());
            holder.tvQuantity.setText("x" + item.getQuantity());
            holder.tvPrice.setText(item.getFormattedSubtotal());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvQuantity, tvPrice;

            ItemViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvProductName);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvPrice = itemView.findViewById(R.id.tvPrice);
            }
        }
    }
}
