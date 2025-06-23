package com.example.chat.friend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chat.R;
import com.example.chat.navigation.fragment.A_1_Function.ChatActivity;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.example.chat.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendInfoActivity extends AppCompatActivity {
    private ImageView friendAvatar;
    private TextView friendUsernameText;
    private TextView friendRemark;
    private String friendUsername;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_info);

        // 初始化视图
        friendAvatar = findViewById(R.id.friend_avatar);
        friendUsernameText = findViewById(R.id.friend_username);
        friendRemark = findViewById(R.id.friend_remark);

        // 获取传递的好友信息
        friendUsername = getIntent().getStringExtra("friendUsername");
        currentUsername = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);

        if (friendUsername == null || currentUsername == null) {
            Toast.makeText(this, "获取用户信息失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 加载好友信息
        loadFriendInfo();
        // 设置返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        // 设置备注点击事件
        findViewById(R.id.remark_layout).setOnClickListener(v -> showRemarkDialog());
        // 设置发送消息按钮
        findViewById(R.id.btn_send_message).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("friendUsername", friendUsername);
            startActivity(intent);
            finish();
        });

        // 设置头像点击事件
        friendAvatar.setOnClickListener(v -> {
        });
    }

    private void loadFriendInfo() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Map<String, Object>> call = apiService.getFriendInfo(currentUsername, friendUsername);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> friendInfo = response.body();

                    // 设置用户名
                    String nickname = (String) friendInfo.get("nickname");
                    friendUsernameText.setText(nickname);

                    // 设置备注
                    String remark = (String) friendInfo.get("remark");
                    friendRemark.setText(remark != null && !remark.isEmpty() ? remark : "设置备注");

                    // 加载头像
                    String userPhoto = (String) friendInfo.get("userPhoto");
                    if (userPhoto != null && !userPhoto.isEmpty()) {
                        String photoUrl = Constants.USER_PHOTO_BASE_URL + userPhoto + "?v=" + System.currentTimeMillis();
                        Glide.with(FriendInfoActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(friendAvatar);
                    }
                } else {
                    Toast.makeText(FriendInfoActivity.this, "获取好友信息失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(FriendInfoActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRemarkDialog() {
        EditText input = new EditText(this);
        String currentRemark = friendRemark.getText().toString();
        if (!"设置备注".equals(currentRemark)) {
            input.setText(currentRemark);
        }

        new AlertDialog.Builder(this)
                .setTitle("设置备注")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newRemark = input.getText().toString().trim();
                    updateRemark(newRemark);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateRemark(String newRemark) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, String> payload = new HashMap<>();
        payload.put("username", currentUsername);
        payload.put("friendUsername", friendUsername);
        payload.put("remark", newRemark);

        Call<ResponseBody> call = apiService.updateFriendRemark(payload);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    friendRemark.setText(newRemark.isEmpty() ? "设置备注" : newRemark);
                    Toast.makeText(FriendInfoActivity.this, "备注更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FriendInfoActivity.this, "备注更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(FriendInfoActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 