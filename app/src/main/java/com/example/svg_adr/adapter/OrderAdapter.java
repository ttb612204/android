package com.example.svg_adr.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.svg_adr.R;
import com.example.svg_adr.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orders;
    private OnOrderClickListener listener;
    private boolean isStoreView; // For store/admin view

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(Context context, OnOrderClickListener listener) {
        this(context, listener, false);
    }

    public OrderAdapter(Context context, OnOrderClickListener listener, boolean isStoreView) {
        this.context = context;
        this.orders = new ArrayList<>();
        this.listener = listener;
        this.isStoreView = isStoreView;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvStoreName, tvStatus, tvItemsPreview, tvDate, tvTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvItemsPreview = itemView.findViewById(R.id.tvItemsPreview);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }

        void bind(Order order) {
            if (isStoreView) {
                // Show customer name instead of store name
                tvStoreName.setText(order.getUserName() != null ? order.getUserName() : "Khách hàng");
            } else {
                tvStoreName.setText(order.getStoreName());
            }

            tvStatus.setText(order.getStatusText());
            tvDate.setText(order.getFormattedDate());
            tvTotal.setText(order.getFormattedTotal());

            // Items preview
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                StringBuilder preview = new StringBuilder();
                for (int i = 0; i < Math.min(3, order.getItems().size()); i++) {
                    if (i > 0)
                        preview.append(", ");
                    preview.append(order.getItems().get(i).getProductName());
                }
                if (order.getItems().size() > 3) {
                    preview.append("...");
                }
                tvItemsPreview.setText(preview.toString());
            }

            // Status color
            int statusColor = getStatusColor(order.getStatus());
            GradientDrawable bgDrawable = (GradientDrawable) tvStatus.getBackground();
            bgDrawable.setColor(statusColor);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(order);
                }
            });
        }

        private int getStatusColor(String status) {
            switch (status) {
                case Order.STATUS_PENDING:
                    return ContextCompat.getColor(context, R.color.status_pending);
                case Order.STATUS_CONFIRMED:
                    return ContextCompat.getColor(context, R.color.status_confirmed);
                case Order.STATUS_SHIPPING:
                    return ContextCompat.getColor(context, R.color.status_shipping);
                case Order.STATUS_COMPLETED:
                    return ContextCompat.getColor(context, R.color.status_completed);
                case Order.STATUS_CANCELLED:
                    return ContextCompat.getColor(context, R.color.status_cancelled);
                default:
                    return ContextCompat.getColor(context, R.color.secondary);
            }
        }
    }
}
