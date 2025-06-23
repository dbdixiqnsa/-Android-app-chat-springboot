package com.example.chat.navigation.fragment.C_3_Function;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.R;
import com.example.chat.navigation.fragment.C_3_Function.details.ChangeEmailActivity;
import com.example.chat.navigation.fragment.C_3_Function.details.ChangeSecurityActivity;
import com.example.chat.navigation.fragment.C_3_Function.details.VerifySecurityQuestionsActivity;

public class PrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        // 返回按钮
        ImageView arrowLeft = findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 修改邮箱按钮
        RelativeLayout changeEmailButton = findViewById(R.id.change_email);
        changeEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PrivacyActivity.this, ChangeEmailActivity.class);
                startActivity(intent);
            }
        });

        // 修改密码按钮
        RelativeLayout changePasswordButton = findViewById(R.id.change_password);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动 VerifySecurityQuestionsActivity 而不是直接启动 ChangePasswordActivity
                Intent intent = new Intent(PrivacyActivity.this, VerifySecurityQuestionsActivity.class);
                startActivity(intent);
            }
        });

        // 修改密保按钮
        RelativeLayout changeSecurityButton = findViewById(R.id.change_security);
        changeSecurityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PrivacyActivity.this, ChangeSecurityActivity.class);
                startActivity(intent);
            }
        });
    }
}
