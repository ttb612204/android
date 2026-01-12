package com.example.svg_adr.ui.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.svg_adr.R;
import com.example.svg_adr.model.CartItem;
import com.example.svg_adr.model.Favorite;
import com.example.svg_adr.model.Product;
import com.example.svg_adr.model.Review;
import com.example.svg_adr.model.Store;
import com.example.svg_adr.utils.CartManager;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView ivProductImage, btnMinus, btnPlus, btnFavorite;
    private TextView tvPrice, tvProductName, tvRating, tvSoldCount, tvStoreName, tvDescription, tvQuantity;
    private TextView tvReviewCount;
    private RecyclerView rvReviews;
    private LinearLayout layoutNoReviews;
    private MaterialButton btnAddToCart;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private ReviewAdapter reviewAdapter;

    private String productId;
    private String storeId;
    private String storeName;
    private Product currentProduct;
    private Store currentStore;
    private int quantity = 1;
    private boolean isFavorite = false;
    private String favoriteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra(Constants.EXTRA_PRODUCT_ID);
        storeId = getIntent().getStringExtra(Constants.EXTRA_STORE_ID);
        storeName = getIntent().getStringExtra("store_name");

        if (productId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        cartManager = new CartManager(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadProduct();
        checkFavorite();
        loadReviews();
    }

    private void initViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        tvPrice = findViewById(R.id.tvPrice);
        tvProductName = findViewById(R.id.tvProductName);
        tvRating = findViewById(R.id.tvRating);
        tvSoldCount = findViewById(R.id.tvSoldCount);
        tvStoreName = findViewById(R.id.tvStoreName);
        tvDescription = findViewById(R.id.tvDescription);
        tvQuantity = findViewById(R.id.tvQuantity);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnFavorite = findViewById(R.id.btnFavorite);
        progressBar = findViewById(R.id.progressBar);

        // Reviews
        tvReviewCount = findViewById(R.id.tvReviewCount);
        rvReviews = findViewById(R.id.rvReviews);
        layoutNoReviews = findViewById(R.id.layoutNoReviews);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chi tiết");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupListeners() {
        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        btnAddToCart.setOnClickListener(v -> addToCart());

        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        }
    }

    private void checkFavorite() {
        String userId = sessionManager.getUserId();
        if (userId == null || productId == null)
            return;

        db.collection(Constants.COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", productId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        isFavorite = true;
                        favoriteId = doc.getId();
                        updateFavoriteIcon();
                        return;
                    }
                    isFavorite = false;
                    favoriteId = null;
                    updateFavoriteIcon();
                });
    }

    private void updateFavoriteIcon() {
        if (btnFavorite != null) {
            btnFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        }
    }

    private void toggleFavorite() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavorite && favoriteId != null) {
            // Remove from favorites
            db.collection(Constants.COLLECTION_FAVORITES)
                    .document(favoriteId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        isFavorite = false;
                        favoriteId = null;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add to favorites
            if (currentProduct == null)
                return;

            Favorite favorite = new Favorite(userId, currentProduct, currentStore);
            db.collection(Constants.COLLECTION_FAVORITES)
                    .add(favorite.toMap())
                    .addOnSuccessListener(docRef -> {
                        isFavorite = true;
                        favoriteId = docRef.getId();
                        updateFavoriteIcon();
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void loadProduct() {
        showLoading(true);
        db.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        currentProduct = documentSnapshot.toObject(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setId(documentSnapshot.getId());
                            displayProduct();
                            loadStore();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadStore() {
        if (storeId == null)
            return;

        db.collection(Constants.COLLECTION_STORES)
                .document(storeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentStore = documentSnapshot.toObject(Store.class);
                        if (currentStore != null) {
                            currentStore.setId(documentSnapshot.getId());
                            tvStoreName.setText(currentStore.getName());
                        }
                    }
                });
    }

    private void loadReviews() {
        if (productId == null)
            return;

        // Simple query without orderBy to avoid needing composite index
        db.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Review review = doc.toObject(Review.class);
                            if (review != null) {
                                review.setId(doc.getId());
                                reviews.add(review);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Sort by createdAt descending on client side (safe sort)
                    reviews.sort((r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));

                    // Limit to 10 reviews
                    List<Review> displayList = reviews;
                    if (displayList.size() > 10) {
                        displayList = displayList.subList(0, 10);
                    }

                    reviewAdapter.setReviews(displayList);
                    tvReviewCount.setText(reviews.size() + " đánh giá");

                    if (reviews.isEmpty()) {
                        layoutNoReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                    } else {
                        layoutNoReviews.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    layoutNoReviews.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.GONE);
                });
    }

    private void displayProduct() {
        tvProductName.setText(currentProduct.getName());
        tvPrice.setText(currentProduct.getFormattedPrice());
        tvRating.setText(String.format("%.1f (%d đánh giá)",
                currentProduct.getRating(), currentProduct.getRatingCount()));
        tvSoldCount.setText("Đã bán " + currentProduct.getSoldCount());
        tvDescription.setText(currentProduct.getDescription());
        tvStoreName.setText(storeName != null ? storeName : "Cửa hàng");

        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentProduct.getImageUrl())
                    .placeholder(R.drawable.ic_product)
                    .error(R.drawable.ic_product)
                    .centerCrop()
                    .into(ivProductImage);
        }
    }

    private void addToCart() {
        if (currentProduct == null)
            return;

        CartItem cartItem = new CartItem(currentProduct, currentStore, quantity);
        cartManager.addItem(cartItem);

        Toast.makeText(this, "Đã thêm " + quantity + " sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Review Adapter
    class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private List<Review> reviews = new ArrayList<>();

        void setReviews(List<Review> reviews) {
            this.reviews = reviews;
            notifyDataSetChanged();
        }

        @Override
        public ReviewViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_review, parent, false);
            return new ReviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ReviewViewHolder holder, int position) {
            holder.bind(reviews.get(position));
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        class ReviewViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvDate, tvComment;
            RatingBar ratingBar;

            ReviewViewHolder(View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvComment = itemView.findViewById(R.id.tvComment);
                ratingBar = itemView.findViewById(R.id.ratingBar);
            }

            void bind(Review review) {
                tvUserName.setText(review.getUserName());
                ratingBar.setRating(review.getRating());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(review.getCreatedAt())));

                if (review.getComment() != null && !review.getComment().isEmpty()) {
                    tvComment.setText(review.getComment());
                    tvComment.setVisibility(View.VISIBLE);
                } else {
                    tvComment.setVisibility(View.GONE);
                }
            }
        }
    }
}
