package com.example.demo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {
    ProgressBar progressBar;
    private ImageView imageView;
    private LinearLayout lldn,lldk;
    private FirebaseAuth auth; // Sử dụng Firebase Authentication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);


        auth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không thể kết nối mạng, vui lòng kiểm tra lại!", Toast.LENGTH_LONG).show();
            return; // Dừng nếu không có mạng
        }
        FirebaseUser currentUser = auth.getCurrentUser(); // Lấy user từ Firebase Auth
        // Tạo hiệu ứng nhấp nháy
        imageView = findViewById(R.id.imageView);
        AlphaAnimation blinkAnimation = new AlphaAnimation(1.0f, 0.0f); // Từ hiện sang mờ
        blinkAnimation.setDuration(10000); // Thời gian nhấp nháy là 10000ms
        blinkAnimation.setRepeatCount(Animation.INFINITE); // Lặp lại vô hạn
        blinkAnimation.setRepeatMode(Animation.REVERSE); // Lặp theo kiểu ngược lại

        // Áp dụng hiệu ứng cho ImageView
        imageView.startAnimation(blinkAnimation);
        //tạo hiệu ứng cho icon
        lldn = findViewById(R.id.lldn);
        lldk = findViewById(R.id.lldk);
        // Áp dụng hiệu ứng bounce
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        lldk.startAnimation(bounce);
        lldn.startAnimation(bounce);

    }

    //Kiểm tra kết nối
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void login(View view) {
        if (isNetworkAvailable()) {
            startActivity(new Intent(HomeActivity.this, LoginAcitivity.class));
        } else {
            Toast.makeText(this, "Không có kết nối mạng!", Toast.LENGTH_SHORT).show();
        }
    }

    public void registration(View view) {
        if (isNetworkAvailable()) {
            startActivity(new Intent(HomeActivity.this, RegistrationActivity.class));
        } else {
            Toast.makeText(this, "Không có kết nối mạng!", Toast.LENGTH_SHORT).show();
        }
    }
}
