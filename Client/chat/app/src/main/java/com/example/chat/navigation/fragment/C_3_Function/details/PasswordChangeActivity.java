package com.example.chat.navigation.fragment.C_3_Function.details;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class PasswordChangeActivity extends AppCompatActivity {

    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private TextView confirmChangeButton;
    private LinearLayout arrowLeftContainer;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change);

        // 初始化视图
        arrowLeftContainer = findViewById(R.id.arrow_left_container);
        arrowLeftContainer.setOnClickListener(v -> finish());

        newPasswordEditText = findViewById(R.id.new_password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        confirmChangeButton = findViewById(R.id.confirm_change_button);

        // 获取用户名
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        username = prefs.getString("username", "");

        // 设置按钮点击事件
        confirmChangeButton.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建请求参数
        JSONObject payload = new JSONObject();
        try {
            payload.put("username", username);
            payload.put("newPassword", newPassword);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show();
            return;
        }

        // 发送请求到服务器修改密码
        OkHttpClient client = new OkHttpClient();
        MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);

        Request request = new Request.Builder()
                .url(Constants.UPDATE_PASSWORD_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(PasswordChangeActivity.this, "修改失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(PasswordChangeActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                        finish(); // 关闭当前活动
                    } else {
                        try {
                            String errorBody = response.body().string();
                            Toast.makeText(PasswordChangeActivity.this, "修改失败: " + errorBody, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(PasswordChangeActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
