package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.navigation.fragment.C_1_Function.EditPhotoActivity;
import com.example.chat.navigation.fragment.C_1_Function.EditNameActivity;
import com.example.chat.utils.Constants;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InformationActivity extends AppCompatActivity {

    private TextView nicknameTextView;
    private TextView usernameTextView;
    private ImageView userImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        nicknameTextView = findViewById(R.id.nickname_text);
        usernameTextView = findViewById(R.id.username);
        userImageView = findViewById(R.id.image_user);

        RelativeLayout editNicknameLayout = findViewById(R.id.edit_nickname);
        RelativeLayout editUsernameLayout = findViewById(R.id.edit_username);
        RelativeLayout editPhotoLayout = findViewById(R.id.edit_photo);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        // 设置点击事件
        arrowLeftContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 获取用户名和昵称
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);
        String nickname = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("nickname", "默认名字");

        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "用户名未提供", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 显示用户名和昵称
        if (usernameTextView != null) {
            usernameTextView.setText(username);
        }
        if (nicknameTextView != null) {
            nicknameTextView.setText(nickname);
        }

        // 加载用户头像
        loadUserDetails(username);

        // 设置点击事件：点击 edit_nickname 跳转到 EditNameActivity
        editNicknameLayout.setOnClickListener(v -> {
            Intent intent = new Intent(InformationActivity.this, EditNameActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // 设置点击事件：点击 edit_username 跳转到 EditUsernameActivity
//        editUsernameLayout.setOnClickListener(v -> {
//            Intent intent = new Intent(InformationActivity.this, EditUsernameActivity.class);
//            intent.putExtra("username", username);
//            startActivity(intent);
//        });

        // 设置点击事件：点击 edit_photo 跳转到 EditPhotoActivity
        editPhotoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(InformationActivity.this, EditPhotoActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserDetails(String username) {
        OkHttpClient client = new OkHttpClient();
        String url = Constants.USER_DETAILS_URL;

        // 构建请求体，包含用户名
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String jsonRequestBody = "{ \"username\": \"" + username + "\" }";
        RequestBody requestBody = RequestBody.create(jsonRequestBody, JSON);

        // 构建 POST 请求
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(InformationActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String nickname = json.optString("nickname", "默认名字");

                        runOnUiThread(() -> {
                            nicknameTextView.setText(nickname);

                            // 更新 SharedPreferences
                            getSharedPreferences("user_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("nickname", nickname)
                                    .apply();

                            // 加载头像
                            loadUserPhoto();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(InformationActivity.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(InformationActivity.this, "加载失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void loadUserPhoto() {
        // 从 SharedPreferences 中获取头像的本地路径
        String userPhotoPath = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userPhotoPath", null);

        File photoFile = null;
        if (userPhotoPath != null) {
            photoFile = new File(userPhotoPath);
        }

        if (photoFile != null && photoFile.exists()) {
            // 使用 Glide 加载本地存储的图片
            Glide.with(this)
                    .load(photoFile)
                    .placeholder(R.drawable.ic_user)
                    .signature(new ObjectKey(System.currentTimeMillis())) // 强制 Glide 重新加载图片
                    .into(userImageView);
        } else {
            // 如果本地没有图片，则使用默认头像或服务器头像
            Glide.with(this)
                    .load(Constants.USER_PHOTO_BASE_URL + "default.jpg")
                    .placeholder(R.drawable.ic_user)
                    .into(userImageView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从 SharedPreferences 中获取最新的 nickname、username 并更新显示
        String nickname = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("nickname", "默认名字");
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);

        if (nicknameTextView != null) {
            nicknameTextView.setText(nickname);
        }

        if (usernameTextView != null) {
            usernameTextView.setText(username);
        }

        // 加载本地头像
        loadUserPhoto();
    }
}
