package com.example.chat.navigation.fragment.C_3_Function.details;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.R;
import com.example.chat.navigation.fragment.C_3_Function.PrivacyActivity;
import com.example.chat.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChangeEmailActivity extends AppCompatActivity {

    private TextView currentEmailTextView;
    private TextInputEditText newEmailEditText;
    private TextInputLayout newEmailInputLayout;
    private TextView changeEmailButton;
    private String currentEmail;

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        // 初始化视图
        currentEmailTextView = findViewById(R.id.current_email);
        newEmailEditText = findViewById(R.id.new_email);
        newEmailInputLayout = findViewById(R.id.new_email_input_layout);
        changeEmailButton = findViewById(R.id.change_email_button);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        // 设置点击事件
        arrowLeftContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        newEmailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                String error = validateEmail(email);
                if (error != null) {
                    newEmailInputLayout.setError(error);
                } else {
                    newEmailInputLayout.setError(null);
                    newEmailInputLayout.setHelperText(getString(R.string.email_format_valid));
                }
            }
        });

        // 从 Intent 获取当前邮箱，如果没有则从 SharedPreferences 获取
        currentEmail = getIntent().getStringExtra("current_email");
        if (currentEmail == null || currentEmail.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            currentEmail = prefs.getString("email", ""); // 从 SharedPreferences 获取 email
        }

        if (currentEmail.isEmpty()) {
            Toast.makeText(this, "当前邮箱未提供", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 显示当前邮箱
        currentEmailTextView.setText("当前邮箱: " + currentEmail); // 设置格式 "当前邮箱: 邮箱"

        // 设置更改按钮点击事件
        changeEmailButton.setOnClickListener(v -> {
            String newEmail = newEmailEditText.getText().toString().trim();
            String error = validateEmail(newEmail);

            if (error != null) {
                newEmailInputLayout.setError(error);
                return;
            }

            if (newEmail.equals(currentEmail)) {
                newEmailInputLayout.setError("新邮箱不能与当前邮箱相同");
                return;
            }

            updateEmail(newEmail);
        });
    }

    private String validateEmail(String email) {
        if (email.isEmpty()) {
            return getString(R.string.error_email_empty);
        }
        if (!email.contains("@")) {
            return getString(R.string.error_email_at_symbol);
        }
        if (!email.contains(".")) {
            return getString(R.string.error_email_dot);
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return getString(R.string.error_email_format);
        }
        if (parts[0].isEmpty()) {
            return getString(R.string.error_email_local_part);
        }
        if (parts[1].isEmpty() || !parts[1].contains(".")) {
            return getString(R.string.error_email_domain);
        }
        if (!email.matches(EMAIL_PATTERN)) {
            return getString(R.string.error_email_invalid_chars);
        }
        return null;
    }

    private void updateEmail(String newEmail) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // 创建 JSON 请求体
        JSONObject json = new JSONObject();
        try {
            json.put("currentEmail", currentEmail);
            json.put("newEmail", newEmail);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(Constants.UPDATE_EMAIL_URL)
                .post(body)
                .build();

        // 异步网络请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ChangeEmailActivity.this, "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ChangeEmailActivity.this, "邮箱更新成功", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ChangeEmailActivity.this, PrivacyActivity.class);
                        startActivity(intent);
                        newEmailEditText.setText("");

                        // 更新 SharedPreferences 中的邮箱
                        SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
                        editor.putString("email", newEmail); // 保存新的邮箱
                        editor.apply();

                        // 更新当前邮箱的显示
                        currentEmail = newEmail; // 更新实例变量
                        currentEmailTextView.setText("当前邮箱: " + newEmail); // 更新 UI 格式

                        finish();
                    } else {
                        Toast.makeText(ChangeEmailActivity.this, "更新失败: " + responseBody, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
