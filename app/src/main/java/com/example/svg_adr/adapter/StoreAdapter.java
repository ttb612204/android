package com.example.svg_adr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.svg_adr.R;
import com.example.svg_adr.model.Store;

import java.util.ArrayList;
import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private Context context;
    private List<Store> stores;
    private OnStoreClickListener listener;

    public interface OnStoreClickListener {
        void onStoreClick(Store store);
    }

    public StoreAdapter(Context context, OnStoreClickListener listener) {
        this.context = context;
        this.stores = new ArrayList<>();
        this.listener = listener;
    }

    public void setStores(List<Store> stores) {
        this.stores = stores;
        notifyDataSetChanged();
    }

    public void filterByName(String query) {
        // This method can be used for local filtering
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = stores.get(position);
        holder.bind(store);
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    class StoreViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStoreImage;
        TextView tvStoreName, tvStoreDescription, tvRating, tvOrderCount, tvAddress;

        StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoreImage = itemView.findViewById(R.id.ivStoreImage);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvStoreDescription = itemView.findViewById(R.id.tvStoreDescription);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvOrderCount = itemView.findViewById(R.id.tvOrderCount);
            tvAddress = itemView.findViewById(R.id.tvAddress);
        }

        void bind(Store store) {
            tvStoreName.setText(store.getName());
            tvStoreDescription.setText(store.getDescription());
            tvRating.setText(String.format("%.1f", store.getRating()));
            tvOrderCount.setText(store.getOrderCount() + "+ đơn");
            tvAddress.setText(store.getAddress());

            // Load image
            if (store.getImageUrl() != null && !store.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(store.getImageUrl())
                        .placeholder(R.drawable.ic_store)
                        .error(R.drawable.ic_store)
                        .centerCrop()
                        .into(ivStoreImage);
            } else {
                ivStoreImage.setImageResource(R.drawable.ic_store);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoreClick(store);
                }
            });
        }
    }
}
