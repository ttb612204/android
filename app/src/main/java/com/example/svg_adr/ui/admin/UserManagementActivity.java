package com.example.svg_adr.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.svg_adr.R;
import com.example.svg_adr.model.User;
import com.example.svg_adr.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    private UserAdapter userAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadUsers();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý người dùng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(this, user -> showUserDialog(user));
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadUsers);
        swipeRefresh.setColorSchemeResources(R.color.admin_primary);
    }

    private void loadUsers() {
        showLoading(true);
        db.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setId(doc.getId());
                        // Exclude admin users from the list
                        if (!user.isAdmin()) {
                            users.add(user);
                        }
                    }

                    userAdapter.setUsers(users);
                    layoutEmpty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                    rvUsers.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
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

    private void showUserDialog(User user) {
        String message = "Email: " + user.getEmail() +
                "\nVai trò: " + user.getRole() +
                "\nSĐT: " + (user.getPhone() != null ? user.getPhone() : "N/A") +
                "\nĐịa chỉ: " + (user.getAddress() != null ? user.getAddress() : "N/A");

        new AlertDialog.Builder(this)
                .setTitle(user.getName())
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .setNegativeButton("Xóa", (dialog, which) -> confirmDelete(user))
                .show();
    }

    private void confirmDelete(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa người dùng " + user.getName() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteUser(user))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteUser(User user) {
        showLoading(true);
        db.collection(Constants.COLLECTION_USERS)
                .document(user.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Inner adapter class
    static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private android.content.Context context;
        private List<User> users = new ArrayList<>();
        private OnUserClickListener listener;

        interface OnUserClickListener {
            void onUserClick(User user);
        }

        UserAdapter(android.content.Context context, OnUserClickListener listener) {
            this.context = context;
            this.listener = listener;
        }

        void setUsers(List<User> users) {
            this.users = users;
            notifyDataSetChanged();
        }

        @Override
        public UserViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(context)
                    .inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            holder.bind(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView tvName, tvEmail, tvRole;

            UserViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                tvRole = itemView.findViewById(R.id.tvRole);
            }

            void bind(User user) {
                tvName.setText(user.getName());
                tvEmail.setText(user.getEmail());

                String roleText;
                switch (user.getRole()) {
                    case Constants.ROLE_ADMIN:
                        roleText = "Quản trị viên";
                        break;
                    case Constants.ROLE_STORE:
                        roleText = "Chủ cửa hàng";
                        break;
                    default:
                        roleText = "Khách hàng";
                }
                tvRole.setText(roleText);

                itemView.setOnClickListener(v -> {
                    if (listener != null)
                        listener.onUserClick(user);
                });
            }
        }
    }
}
