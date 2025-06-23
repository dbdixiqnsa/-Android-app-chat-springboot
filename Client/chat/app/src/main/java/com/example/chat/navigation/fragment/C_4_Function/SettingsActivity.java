package com.example.chat.navigation.fragment.C_4_Function;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.R;
import com.example.chat.navigation.fragment.C_4_Function.details.AboutActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import com.example.chat.auth.AuthActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_c_activity_settings);

        // 获取选项视图
        TextView aboutOption = findViewById(R.id.option_about);
        TextView logoutOption = findViewById(R.id.option_logout);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        // 设置点击事件
        arrowLeftContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        aboutOption.setOnClickListener(v -> showAbout());
        logoutOption.setOnClickListener(v -> showLogoutDialog());
    }

    private void showAbout() {
        Intent aboutIntent = new Intent(this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    private void showLogoutDialog() {
        // 创建并显示退出确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("退出账号");
        builder.setMessage("确定要退出当前账号吗？");

        // 设置确定按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 跳转到登录页面
                Intent authIntent = new Intent(SettingsActivity.this, AuthActivity.class);
                // 清除当前栈中的所有活动
                authIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(authIntent);
                finish();
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
