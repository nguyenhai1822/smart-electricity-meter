package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private LinearLayout llSetting, llHelp, llGt, llOut;
    private TextView userNameTextView; // TextView hiển thị tên người dùng

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private static final String SELECTED_ITEM = "selected_item";
    private int selectedItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gd_user);

        initViews(); // Khởi tạo các view
        setupFirebase(); // Khởi tạo Firebase
        fetchUserName(); // Lấy tên người dùng từ Firebase
        setListeners(); // Gắn sự kiện click cho LinearLayout
        // BottomNavigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(SELECTED_ITEM, R.id.ng_home);
        } else {
            selectedItemId = R.id.ng_home;
        }
        bottomNavigationView.setSelectedItemId(selectedItemId);
        setupBottomNavigationView(bottomNavigationView);
    }

    // Khởi tạo các View từ layout
    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        llSetting = findViewById(R.id.ll_setting);
        llHelp = findViewById(R.id.ll_help);
        llGt = findViewById(R.id.ll_gt);
        llOut = findViewById(R.id.ll_out);
        userNameTextView = findViewById(R.id.user_name); // Ánh xạ TextView từ layout

        // Đặt tab mặc định là "Profile"
        bottomNavigationView.setSelectedItemId(R.id.ng_profile);
    }

    // Khởi tạo Firebase
    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    // Lấy tên người dùng từ Firebase và hiển thị vào TextView
    private void fetchUserName() {
        String userId = auth.getCurrentUser().getUid(); // Lấy ID của người dùng hiện tại
        DatabaseReference userRef = database.getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = snapshot.child("name").getValue(String.class); // Lấy giá trị "name"
                    userNameTextView.setText(userName); // Hiển thị tên lên TextView
                } else {
                    Toast.makeText(UserActivity.this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupBottomNavigationView(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (selectedItemId != itemId) {
                selectedItemId = itemId;

                Intent intent = null;
                if (itemId == R.id.ng_home) {
                    intent = new Intent(UserActivity.this, MainActivity.class);
                } else if (itemId == R.id.ng_bill) {
                    intent = new Intent(UserActivity.this, Bill_Activity.class);
                } else if (itemId == R.id.ng_news) {
                    intent = new Intent(UserActivity.this, Notification_Activity.class);
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
        bottomNavigationView.setSelectedItemId(R.id.ng_profile);
    }


    // Gắn sự kiện cho các LinearLayout
    private void setListeners() {
        llSetting.setOnClickListener(view -> navigateToActivity(ChangePasswordActivity.class));
        llHelp.setOnClickListener(view -> navigateToActivity(Help_Activity.class));
        llGt.setOnClickListener(view -> navigateToActivity(About_Activity.class));
        llOut.setOnClickListener(view -> {
            navigateToActivity(HomeActivity.class);
            finish(); // Đóng UserActivity
        });
    }

    // Chuyển sang Activity khác với tab đã chọn (nếu có)
    private void navigateToActivity(Class<?> activityClass, int selectedTabId) {
        Intent intent = new Intent(UserActivity.this, activityClass);
        intent.putExtra("selected_item", selectedTabId);
        startActivity(intent);
        finish();
    }

    // Chuyển sang Activity khác không cần tab
    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(UserActivity.this, activityClass);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToActivity(MainActivity.class, R.id.ng_profile); // Quay lại tab "Profile"
    }
}
