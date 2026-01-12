package com.example.svg_adr.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.svg_adr.R;
import com.example.svg_adr.model.Favorite;
import com.example.svg_adr.utils.Constants;
import com.example.svg_adr.utils.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private FavoriteAdapter favoriteAdapter;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadFavorites();
    }

    private void initViews() {
        rvFavorites = findViewById(R.id.rvFavorites);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Yêu thích");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        favoriteAdapter = new FavoriteAdapter();
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(favoriteAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadFavorites);
        swipeRefresh.setColorSchemeResources(R.color.primary);
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        db.collection(Constants.COLLECTION_FAVORITES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    List<Favorite> favorites = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Favorite fav = doc.toObject(Favorite.class);
                        fav.setId(doc.getId());
                        favorites.add(fav);
                    }

                    favoriteAdapter.setFavorites(favorites);
                    layoutEmpty.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
                    rvFavorites.setVisibility(favorites.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Favorite Adapter
    class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
        private List<Favorite> favorites = new ArrayList<>();

        void setFavorites(List<Favorite> favorites) {
            this.favorites = favorites;
            notifyDataSetChanged();
        }

        @Override
        public FavoriteViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_favorite, parent, false);
            return new FavoriteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FavoriteViewHolder holder, int position) {
            holder.bind(favorites.get(position));
        }

        @Override
        public int getItemCount() {
            return favorites.size();
        }

        class FavoriteViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProduct, btnRemove;
            TextView tvName, tvStore, tvPrice;

            FavoriteViewHolder(View itemView) {
                super(itemView);
                ivProduct = itemView.findViewById(R.id.ivProduct);
                tvName = itemView.findViewById(R.id.tvName);
                tvStore = itemView.findViewById(R.id.tvStore);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                btnRemove = itemView.findViewById(R.id.btnRemove);
            }

            void bind(Favorite favorite) {
                tvName.setText(favorite.getProductName());
                tvStore.setText(favorite.getStoreName());
                tvPrice.setText(favorite.getFormattedPrice());

                if (favorite.getProductImage() != null && !favorite.getProductImage().isEmpty()) {
                    Glide.with(FavoritesActivity.this)
                            .load(favorite.getProductImage())
                            .placeholder(R.drawable.ic_product)
                            .into(ivProduct);
                }

                btnRemove.setOnClickListener(v -> removeFavorite(favorite));

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(FavoritesActivity.this, ProductDetailActivity.class);
                    intent.putExtra(Constants.EXTRA_PRODUCT_ID, favorite.getProductId());
                    intent.putExtra(Constants.EXTRA_STORE_ID, favorite.getStoreId());
                    startActivity(intent);
                });
            }
        }
    }

    private void removeFavorite(Favorite favorite) {
        db.collection(Constants.COLLECTION_FAVORITES)
                .document(favorite.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    loadFavorites();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
