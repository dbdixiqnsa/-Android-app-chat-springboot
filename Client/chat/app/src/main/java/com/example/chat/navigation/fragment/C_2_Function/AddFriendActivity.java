package com.example.chat.navigation.fragment.C_2_Function;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chat.R;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddFriendActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private View arrowLeftContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Toolbar toolbar = findViewById(R.id.toolbar);

        // 获取返回按钮容器并设置点击事件
        arrowLeftContainer = toolbar.findViewById(R.id.arrow_left_container);
        arrowLeftContainer.setOnClickListener(v -> {
            // 执行返回操作
            onBackPressed();
        });

        usernameEditText = findViewById(R.id.username_input);
        Button sendRequestButton = findViewById(R.id.send_request_button);


        sendRequestButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            if (!username.isEmpty()) {
                sendAddFriendRequest(username);
            } else {
                Toast.makeText(AddFriendActivity.this, "请输入用户名", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendAddFriendRequest(String toUsername) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String fromUsername = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);

        if (fromUsername == null) {
            Toast.makeText(AddFriendActivity.this, "当前用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromUsername.equals(toUsername)) {
            Toast.makeText(AddFriendActivity.this, "不能添加自己为好友", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("fromUsername", fromUsername);
        payload.put("toUsername", toUsername);

        Call<Map<String, String>> call = apiService.addFriendRequest(payload);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    Toast.makeText(AddFriendActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        // 使用 Gson 解析错误响应
                        Gson gson = new Gson();
                        Map<String, String> errorBody = gson.fromJson(response.errorBody().string(), Map.class);
                        String errorMsg = errorBody.get("message"); // 获取 "message" 字段
                        Toast.makeText(AddFriendActivity.this, errorMsg != null ? errorMsg : "发送失败", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(AddFriendActivity.this, "发送失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(AddFriendActivity.this, "请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
