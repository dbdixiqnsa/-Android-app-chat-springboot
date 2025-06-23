package com.example.chat.navigation.fragment.C_1_Function;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.View;
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

public class EditNameActivity extends AppCompatActivity {

    private EditText editName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);

        // 初始化视图
        editName = findViewById(R.id.edit_name);
        Button saveButton = findViewById(R.id.save_button);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        arrowLeftContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "未提供用户名", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> {
            String newName = editName.getText().toString();
            if (newName.isEmpty()) {
                Toast.makeText(this, "名字不能为空", Toast.LENGTH_SHORT).show();
            } else {
                updateNickname(username, newName);
            }
        });
    }

    private void updateNickname(String username, String nickname) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // 创建 JSON 请求体
        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("nickname", nickname);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(Constants.UPDATE_NICKNAME_URL)
                .post(body)
                .build();

        // 异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EditNameActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        // 保存新昵称到 SharedPreferences
                        getSharedPreferences("user_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("nickname", nickname)
                                .apply();

                        Toast.makeText(EditNameActivity.this, "修改成功 ", Toast.LENGTH_SHORT).show();
                        finish(); // 返回上一页
                    } else {
                        Toast.makeText(EditNameActivity.this, "修改失败 " + responseBody, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
