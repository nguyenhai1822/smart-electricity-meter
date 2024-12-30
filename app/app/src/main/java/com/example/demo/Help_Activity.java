package com.example.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class Help_Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gd_help);
        // Đảm bảo ép kiểu đúng là CardView
        androidx.cardview.widget.CardView emailLayout = findViewById(R.id.email_layout);
        emailLayout.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:")); // Chỉ định ứng dụng email
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"nguyenhai01082002@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ khách hàng");
            startActivity(Intent.createChooser(emailIntent, "Gửi email qua:"));
        });

        // Hotline CSKH
        androidx.cardview.widget.CardView hotlineLayout = findViewById(R.id.hotline_layout);
        hotlineLayout.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:835211804")); // Số điện thoại
            startActivity(callIntent);
        });

        // Zalo
        androidx.cardview.widget.CardView zaloLayout = findViewById(R.id.zalo_layout);
        zaloLayout.setOnClickListener(v -> {
            Intent zaloIntent = new Intent(Intent.ACTION_VIEW);
            zaloIntent.setData(Uri.parse("https://zalo.me/0835211804")); // Link Zalo
            startActivity(zaloIntent);
        });

    }
}
