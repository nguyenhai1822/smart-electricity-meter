package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SELECTED_ITEM = "selected_item";
    private int selectedItemId;

    // Views
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<String> deviceList;
    private ImageView emptyBox;
    private TextView noDeviceText;
    private Button addButton;
    private FloatingActionButton fabAddDevice;

    // Firebase
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;
    private boolean isAdmin = false;
    // Danh sách email admin
    private List<String> adminEmails = Arrays.asList(
            "nguyenhai01082002@gmail.com", // Email admin
            "admin2@example.com"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // BottomNavigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(SELECTED_ITEM, R.id.ng_home);
        } else {
            selectedItemId = R.id.ng_home;
        }
        bottomNavigationView.setSelectedItemId(selectedItemId);
        setupBottomNavigationView(bottomNavigationView);

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view);
        emptyBox = findViewById(R.id.empty_box);
        noDeviceText = findViewById(R.id.no_device_text);
        addButton = findViewById(R.id.btn_add);
        fabAddDevice = findViewById(R.id.btn_add_device);

        deviceList = new ArrayList<>();
        adapter = new DeviceAdapter(deviceList, this::removeDevice);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateUIState();

        addButton.setOnClickListener(view -> showAddDeviceDialog());
        fabAddDevice.setOnClickListener(view -> showAddDeviceDialog());

        databaseReference = FirebaseDatabase.getInstance().getReference("devices");
        // Lấy thông tin tài khoản hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            isAdmin = adminEmails.contains(currentUser.getEmail());
            fabAddDevice.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        } else {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            fabAddDevice.setVisibility(View.GONE);
        }
        loadDevicesFromFirebase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseReference != null && childEventListener != null) {
            databaseReference.removeEventListener(childEventListener);
        }
    }

    private void loadDevicesFromFirebase() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e(TAG, "User is not authenticated. Cannot load devices.");
            Toast.makeText(this, "Bạn cần đăng nhập để xem thiết bị.", Toast.LENGTH_SHORT).show();
            return;
        }

        deviceList.clear();
        updateUIState();

        // Tham chiếu tới "devices" và lọc theo owner
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String deviceName = snapshot.child("name").getValue(String.class);
                if (deviceName != null) {
                    // Loại bỏ hậu tố "(ad)" nếu có
                    String displayName = deviceName.replace("(ad)", "").trim();

                    if (!deviceList.contains(displayName)) {
                        deviceList.add(displayName);
                        int position = deviceList.indexOf(displayName);
                        adapter.notifyItemInserted(position);
                        sortDeviceList();
                        adapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                        updateUIState(); // Cập nhật UI
                    }
                }
            }

            private void sortDeviceList() {
                Collections.sort(deviceList, String::compareToIgnoreCase);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String deviceName = snapshot.child("name").getValue(String.class);
                if (deviceName != null) {
                    int index = deviceList.indexOf(deviceName);
                    if (index != -1) {
                        deviceList.remove(index);
                        adapter.notifyItemRemoved(index);
                        sortDeviceList(); // Sắp xếp danh sách sau khi xóa
                        updateUIState();
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                // Handle khi dữ liệu thiết bị thay đổi nếu cần
                String deviceName = snapshot.child("name").getValue(String.class);
                if (deviceName != null && deviceList.contains(deviceName)) {
                    int index = deviceList.indexOf(deviceName);
                    adapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // Không cần xử lý nếu không cần sắp xếp lại
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        };

        databaseReference.orderByChild("owner").equalTo(currentUserId).addChildEventListener(childEventListener);
    }


    private void updateUIState() {
        runOnUiThread(() -> {
            if (deviceList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyBox.setVisibility(View.VISIBLE);
                noDeviceText.setVisibility(View.VISIBLE);
                fabAddDevice.setVisibility(View.GONE); // Ẩn FAB khi danh sách trống
                addButton.setVisibility(View.VISIBLE); // Hiển thị nút thêm
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyBox.setVisibility(View.GONE);
                noDeviceText.setVisibility(View.GONE);
                fabAddDevice.setVisibility(View.VISIBLE); // Hiện FAB khi có thiết bị
                addButton.setVisibility(View.GONE); // Ẩn nút thêm
            }
            // Cập nhật hiển thị nút thêm dựa trên quyền hạn
            if (isAdmin) {
                fabAddDevice.setVisibility(View.VISIBLE);
            } else {
                boolean canAddDevice = deviceList.size() < 1;
                fabAddDevice.setVisibility(canAddDevice ? View.VISIBLE : View.GONE);
                addButton.setVisibility(canAddDevice ? View.VISIBLE : View.GONE);
            }
        });

    }

    private void setupBottomNavigationView(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (selectedItemId != itemId) {
                selectedItemId = itemId;

                Intent intent = null;
                if (itemId == R.id.ng_bill) {
                    intent = new Intent(MainActivity.this, Bill_Activity.class);
                } else if (itemId == R.id.ng_news) {
                    intent = new Intent(MainActivity.this, Notification_Activity.class);
                } else if (itemId == R.id.ng_profile) {
                    intent = new Intent(MainActivity.this, UserActivity.class);
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

        // Đặt mục được chọn cho MainActivity
        bottomNavigationView.setSelectedItemId(R.id.ng_home);
    }

    private void removeDevice(int position) {
        if (position < 0 || position >= deviceList.size()) {
            Toast.makeText(this, "Vị trí không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        String removedDevice = deviceList.get(position);

        // Xóa thiết bị từ Firebase
        databaseReference.orderByChild("name").equalTo(isAdmin ? removedDevice + "(ad)" : removedDevice)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    child.getRef().removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Đã xóa thiết bị", Toast.LENGTH_SHORT).show();
                            // Xóa thiết bị khỏi danh sách cục bộ
                            deviceList.remove(removedDevice);
                            adapter.notifyDataSetChanged(); // Cập nhật lại RecyclerView

                            // Hoặc gọi lại loadDevicesFromFirebase() nếu cần tải lại toàn bộ
                            // loadDevicesFromFirebase();
                            updateUIState();
                        } else {
                            Toast.makeText(MainActivity.this, "Không thể xóa thiết bị từ Firebase.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 102 && resultCode == RESULT_OK && data != null) {
            String qrName = data.getStringExtra("qr_name");
            if (qrName != null) {
                Toast.makeText(this, "Đã thêm thiết bị: " + qrName, Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }


    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm Thiết Bị");
        builder.setMessage("Bạn muốn làm gì?");

        builder.setPositiveButton("Quét mã QR", (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, QR_Activity.class);
            startActivityForResult(intent, 102); // Sử dụng requestCode = 102
        });

        builder.setNegativeButton("Nhập mã", (dialog, which) -> {
            showInputCodeDialog();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showInputCodeDialog() {
        EditText inputCode = new EditText(this);
        inputCode.setHint("Nhập mã thiết bị");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập Mã Thiết Bị");
        builder.setView(inputCode);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String code = inputCode.getText().toString().trim();

            if (!code.isEmpty() && code.startsWith("NTH")) {
                String deviceName = code.substring(3).trim();

                if (!deviceName.isEmpty()) {
                    // Lấy UID của người dùng hiện tại
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    if (currentUserId == null) {
                        Toast.makeText(this, "Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Nếu là admin, thêm hậu tố "ad" vào tên thiết bị
                    if (isAdmin) {
                        deviceName = deviceName + "(ad)";
                    }

                    // Kiểm tra dữ liệu trên Firebase
                    String finalDeviceName = deviceName; // Biến cuối để dùng trong lambda
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            boolean isDuplicate = false;

                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String owner = snapshot.child("owner").getValue(String.class);
                                String name = snapshot.child("name").getValue(String.class);

                                if (name != null && name.equals(finalDeviceName)) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (isDuplicate) {
                                Toast.makeText(MainActivity.this, "Thiết bị \"" + finalDeviceName + "\" đã được sử dụng!", Toast.LENGTH_SHORT).show();
                            } else {
                                // Chuẩn bị dữ liệu thiết bị mới
                                Map<String, Object> deviceData = new HashMap<>();
                                deviceData.put("owner", currentUserId);
                                deviceData.put("name", finalDeviceName);
                                deviceData.put("status", "active"); // trạng thái mặc định

                                // Thêm thiết bị mới vào Firebase
                                databaseReference.child(finalDeviceName).setValue(deviceData).addOnCompleteListener(addTask -> {
                                    if (addTask.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Thành công! Tên thiết bị: " + finalDeviceName, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Lỗi khi thêm thiết bị vào Firebase.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(MainActivity.this, "Lỗi khi kiểm tra dữ liệu trên Firebase!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Tên thiết bị không hợp lệ!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Mã thiết bị không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });




        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}



