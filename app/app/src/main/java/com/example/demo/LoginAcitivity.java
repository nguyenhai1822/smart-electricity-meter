package com.example.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginAcitivity extends AppCompatActivity {
    ProgressBar progressBar;
    private FirebaseAuth auth; // Sử dụng Firebase Authentication
    TextView tvdangki ;
    EditText emaillogin, passwordlogin;
    Button btn_dn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);  // Đảm bảo layout chính xác
        tvdangki = findViewById(R.id.sign_up);

        auth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        emaillogin = findViewById(R.id.email_login);
        passwordlogin = findViewById(R.id.password_login);
        btn_dn = findViewById(R.id.btn_dn);

        FirebaseUser currentUser = auth.getCurrentUser();
//        if (currentUser != null) {
//            progressBar.setVisibility(View.VISIBLE);
//            startActivity(new Intent(LoginAcitivity.this, MainActivity.class));
//            Toast.makeText(this, "Vui lòng đợi, bạn đã đăng nhập rồi", Toast.LENGTH_SHORT).show();
//            finish();
//        }
        tvdangki.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginAcitivity.this, RegistrationActivity.class));
            }
        });
        //hiệu ứng
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce1dp);
        tvdangki.startAnimation(bounce);
        // Nhận dữ liệu email và password từ RegistrationActivity
        Intent intent = getIntent();
        String emailFromRegistration = intent.getStringExtra("email");
        String passwordFromRegistration = intent.getStringExtra("password");
        // Điền dữ liệu vào các trường nếu có
        if (!TextUtils.isEmpty(emailFromRegistration)) {
            emaillogin.setText(emailFromRegistration);
        }
        if (!TextUtils.isEmpty(passwordFromRegistration)) {
            passwordlogin.setText(passwordFromRegistration);
        }
        btn_dn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String email = emaillogin.getText().toString().trim();
                String password = passwordlogin.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginAcitivity.this, "Nhập email", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginAcitivity.this, "Nhập mật khẩu", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginAcitivity.this, task -> {
                            if (task.isSuccessful()) {
                                startActivity(new Intent(LoginAcitivity.this, MainActivity.class));
                                finish();
                            } else {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(LoginAcitivity.this, "Lỗi đăng nhập: email hoặc mật khẩu không chính xác" , Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

    }


    public void registration(View view) {
        startActivity(new Intent(LoginAcitivity.this, RegistrationActivity.class));
    }
}
