package com.example.chat.navigation.fragment.C_3_Function.details;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.R;
import com.example.chat.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private String username; // 当前登录的用户名
    private List<SecurityItem> securityQuestions = new ArrayList<>();

    private LinearLayout arrowLeftContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // 初始化视图
        arrowLeftContainer = findViewById(R.id.arrow_left_container);
        arrowLeftContainer.setOnClickListener(v -> finish());

        // 获取当前用户名（从 SharedPreferences 中获取）
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        username = prefs.getString("username", "");
        if (username.isEmpty()) {
            Toast.makeText(this, "未能获取用户名，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 获取用户的密保问题
        fetchSecurityQuestions();
    }

    // 获取密保问题
    private void fetchSecurityQuestions() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.SECURITY_QUESTIONS_URL + "?username=" + username)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "获取密保失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    securityQuestions = parseSecurityItems(responseBody);
                    runOnUiThread(() -> {
                        if (securityQuestions.size() >= 3) {
                            // 有3个或以上的密保问题，进入密保验证界面
                            Intent intent = new Intent(ChangePasswordActivity.this, VerifySecurityQuestionsActivity.class);
                            intent.putExtra("security_questions", new ArrayList<>(securityQuestions));
                            startActivity(intent);
                            finish(); // 关闭当前活动
                        } else {
                            // 少于3个密保问题，弹出提示框
                            showAddSecurityDialog();
                        }
                    });
                } else {
                    String errorBody = response.body().string();
                    runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, "获取密保失败: " + errorBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // 解析密保问题，只获取问题，不包含答案
    private List<SecurityItem> parseSecurityItems(String responseBody) {
        List<SecurityItem> items = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                items.add(new SecurityItem(obj.getString("question")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    // 显示添加密保问题的对话框
    private void showAddSecurityDialog() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("密保问题不足")
                    .setMessage("您的密保问题不足3个，请前往补充。")
                    .setPositiveButton("前往补充", (dialog, which) -> {
                        // 跳转到添加密保问题的界面
                        Intent intent = new Intent(ChangePasswordActivity.this, ChangeSecurityActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                        // 返回上一级
                        finish();
                    })
                    .setCancelable(false) // 禁止点击外部取消对话框
                    .show();
        });
    }
}
