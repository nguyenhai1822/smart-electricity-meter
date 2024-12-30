package com.example.demo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Notification_Activity extends AppCompatActivity {
    private static final String SELECTED_ITEM = "selected_item";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private int selectedItemId;
    private List<CustomNotification> notificationList;
    private FloatingActionButton fabAdd;

    private DatabaseReference databaseReference;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gd_notification);

        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);

        // Lấy thông tin tài khoản hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(SELECTED_ITEM, R.id.ng_home);
        } else {
            selectedItemId = R.id.ng_home;
        }
        bottomNavigationView.setSelectedItemId(selectedItemId);
        setupBottomNavigationView(bottomNavigationView);

        if (currentUser != null) {
            List<String> adminEmails = Arrays.asList(
                    "nguyenhai01082002@gmail.com", // Email admin
                    "admin2@example.com"
            );

            isAdmin = adminEmails.contains(currentUser.getEmail());
            fabAdd.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        } else {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            fabAdd.setVisibility(View.GONE);
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("notifications");
        notificationList = new ArrayList<>();
        loadNotifications();

        adapter = new NotificationAdapter(notificationList, isAdmin);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(view -> showAddNotificationDialog());
    }

    private void loadNotifications() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String id = data.getKey();
                    String title = data.child("title").getValue(String.class);
                    String content = data.child("content").getValue(String.class);
                    long timestamp = data.child("timestamp").getValue(Long.class);

                    // Lấy trạng thái isNew của người dùng hiện tại
                    Boolean isNew = data.child("users").child(currentUserId).child("isNew").getValue(Boolean.class);
                    if (isNew == null) {
                        isNew = true; // Mặc định là true nếu chưa có trạng thái
                    }

                    notificationList.add(new CustomNotification(id, title, content, isNew, timestamp));
                }

                notificationList.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Notification_Activity.this, "Lỗi khi tải thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showAddNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_notification, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etContent = dialogView.findViewById(R.id.etContent);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = builder.create();

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAdd.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                String id = databaseReference.push().getKey();
                long timestamp = System.currentTimeMillis();
                CustomNotification notification = new CustomNotification(id, title, content, true, timestamp);
                if (id != null) {
                    databaseReference.child(id).setValue(notification)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Thêm thông báo thành công", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi khi thêm thông báo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        dialog.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void setupBottomNavigationView(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (selectedItemId != itemId) {
                selectedItemId = itemId;

                Intent intent = null;
                if (itemId == R.id.ng_home) {
                    intent = new Intent(Notification_Activity.this, MainActivity.class);
                } else if (itemId == R.id.ng_bill) {
                    intent = new Intent(Notification_Activity.this, Bill_Activity.class);
                } else if (itemId == R.id.ng_profile) {
                    intent = new Intent(Notification_Activity.this, UserActivity.class);
                }

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish(); // Đảm bảo Activity hiện tại được kết thúc
                }
            }
            return true;
        });

        // Đặt mục được chọn cho Bill_Activity
        bottomNavigationView.setSelectedItemId(R.id.ng_news);
    }
}
