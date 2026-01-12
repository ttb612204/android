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
import com.example.svg_adr.model.CartItem;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> items;
    private OnCartItemListener listener;

    public interface OnCartItemListener {
        void onQuantityChanged(CartItem item, int newQuantity);

        void onItemDelete(CartItem item);
    }

    public CartAdapter(Context context, OnCartItemListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage, btnMinus, btnPlus, btnDelete;
        TextView tvProductName, tvStoreName, tvPrice, tvQuantity;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(CartItem item) {
            tvProductName.setText(item.getProductName());
            tvStoreName.setText(item.getStoreName());
            tvPrice.setText(item.getFormattedPrice());
            tvQuantity.setText(String.valueOf(item.getQuantity()));

            if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
                Glide.with(context)
                        .load(item.getProductImage())
                        .placeholder(R.drawable.ic_product)
                        .error(R.drawable.ic_product)
                        .centerCrop()
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.ic_product);
            }

            btnMinus.setOnClickListener(v -> {
                if (item.getQuantity() > 1 && listener != null) {
                    listener.onQuantityChanged(item, item.getQuantity() - 1);
                }
            });

            btnPlus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuantityChanged(item, item.getQuantity() + 1);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemDelete(item);
                }
            });
        }
    }
}
