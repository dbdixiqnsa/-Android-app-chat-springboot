package com.example.chat.navigation.fragment.C_1_Function;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.R;
import com.example.chat.utils.Constants;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditUsernameActivity extends AppCompatActivity {

    private EditText editUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_username);

        // 初始化视图
        editUsername = findViewById(R.id.edit_username);
        Button saveButton = findViewById(R.id.save_button);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        arrowLeftContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(this, "未提供当前用户名", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString();
            if (newUsername.isEmpty()) {
                Toast.makeText(this, "账号不能为空", Toast.LENGTH_SHORT).show();
            } else {
                updateUsername(currentUsername, newUsername);
            }
        });
    }

    private void updateUsername(String currentUsername, String newUsername) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        // 创建 JSON 请求体
        JSONObject json = new JSONObject();
        try {
            json.put("currentUsername", currentUsername);
            json.put("newUsername", newUsername);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(Constants.UPDATE_USERNAME_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        // 异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EditUsernameActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        // 保存新用户名到 SharedPreferences
                        getSharedPreferences("user_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("username", newUsername)
                                .apply();

                        Toast.makeText(EditUsernameActivity.this, "修改成功 ", Toast.LENGTH_SHORT).show();
                        finish(); // 返回上一页
                    } else {
                        // 检查是否返回“该账号已被使用”
                        if (responseBody.contains("该账号已被使用")) {
                            Toast.makeText(EditUsernameActivity.this, "该账号已被使用", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditUsernameActivity.this, "保存失败: " + responseBody, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
