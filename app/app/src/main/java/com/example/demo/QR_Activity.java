package com.example.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QR_Activity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;
    private DecoratedBarcodeView barcodeView;
    private Button btnGallery;
    private DatabaseReference databaseReference;
    private boolean isAdmin;
    private List<String> adminEmails = Arrays.asList(
            "nguyenhai01082002@gmail.com", // Email admin
            "admin2@example.com"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gd_qr);

        barcodeView = findViewById(R.id.camera_preview);
        btnGallery = findViewById(R.id.btn_add_img);
        // Lấy thông tin tài khoản hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            isAdmin = adminEmails.contains(currentUser.getEmail());
        } else {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
        }

        // Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("devices");

        checkCameraPermission();

        // Set status view text
        barcodeView.getStatusView().setText("Quét mã QR...");

        // Continuous scanning
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                String qrData = result.getText(); // Dữ liệu từ mã QR
                if (qrData.startsWith("NTH")) { // Kiểm tra 3 ký tự đầu
                    String name = qrData.substring(3).trim(); // Lấy phần còn lại làm tên
                    checkAndAddDevice(name); // Kiểm tra và thêm thiết bị
                } else {
                    Toast.makeText(QR_Activity.this, "Mã QR không hợp lệ!", Toast.LENGTH_SHORT).show();
                    return;
                }

                barcodeView.pause();
            }

            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional: Handle recognition points
            }
        });

        // Button to open gallery
        btnGallery.setOnClickListener(v -> openGallery());
    }
    private void checkAndAddDevice(String name) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalDeviceName = isAdmin ? name + "(ad)" : name;

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean isDuplicate = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String existingName = snapshot.child("name").getValue(String.class);
                    if (existingName != null && existingName.equals(finalDeviceName)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate) {
                    Toast.makeText(QR_Activity.this, "Thiết bị \"" + finalDeviceName + "\" đã được sử dụng!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Map<String, Object> deviceData = new HashMap<>();
                    deviceData.put("owner", currentUserId);
                    deviceData.put("name", finalDeviceName);
                    deviceData.put("status", "active");

                    databaseReference.child(finalDeviceName).setValue(deviceData).addOnCompleteListener(addTask -> {
                        if (addTask.isSuccessful()) {
                            Toast.makeText(QR_Activity.this, "Thành công! Tên thiết bị: " + finalDeviceName, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(QR_Activity.this, "Lỗi khi thêm thiết bị vào Firebase.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(QR_Activity.this, "Lỗi khi kiểm tra dữ liệu trên Firebase!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    String qrData = scanQRFromBitmap(bitmap);
                    if (qrData != null && qrData.startsWith("NTH")) {
                        String name = qrData.substring(3).trim();

                        // Đẩy thông tin lên Firebase
                        databaseReference.child(name).setValue(true).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Đã thêm thiết bị từ ảnh!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Lỗi khi thêm thiết bị vào Firebase!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Không tìm thấy mã QR hợp lệ trong ảnh!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String scanQRFromBitmap(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            com.google.zxing.RGBLuminanceSource source = new com.google.zxing.RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);

            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else {
            Toast.makeText(this, "Quyền camera bị từ chối!", Toast.LENGTH_SHORT).show();
        }
    }
}
