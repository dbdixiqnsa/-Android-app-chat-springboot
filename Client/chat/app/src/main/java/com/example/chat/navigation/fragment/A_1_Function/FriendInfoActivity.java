package com.example.chat.navigation.fragment.A_1_Function;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.MainActivity;
import com.example.chat.R;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendInfoActivity extends AppCompatActivity {
    private static final String TAG = "FriendInfoActivity";

    private ImageView friendAvatar;
    private TextView friendUsernameText;
    private TextView friendRemark;
    private String friendUsername;
    private String currentUsername;
    private AvatarCacheManager cacheManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_info);

        initViews();
        initData();
        setListeners();
    }

    private void initViews() {
        friendAvatar = findViewById(R.id.friend_avatar);
        friendUsernameText = findViewById(R.id.friend_username);
        friendRemark = findViewById(R.id.friend_remark);
    }

    private void initData() {
        // 初始化缓存管理器
        cacheManager = new AvatarCacheManager(this);

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
    }

    private void setListeners() {
        // 设置返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 设置备注点击事件
        findViewById(R.id.remark_layout).setOnClickListener(v -> showRemarkDialog());

        // 修改发送消息按钮的处理逻辑
        findViewById(R.id.btn_send_message).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("friendUsername", friendUsername);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 清除之前的活动
            startActivity(intent);
            finish(); // 结束当前活动
        });

        // 设置删除好友按钮
        findViewById(R.id.btn_delete_friend).setOnClickListener(v -> showDeleteFriendDialog());
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
                    friendRemark.setText(remark != null && !remark.isEmpty() ? remark : "设置标签");

                    // 加载头像
                    String userPhoto = (String) friendInfo.get("userPhoto");
                    if (userPhoto != null && !userPhoto.isEmpty()) {
                        loadAvatar(userPhoto);
                    }
                } else {
                    Toast.makeText(FriendInfoActivity.this, "获取好友信息失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(FriendInfoActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to load friend info", t);
            }
        });
    }

    private void loadAvatar(String userPhoto) {
        // 设置默认头像
        Glide.with(this)
                .load(R.drawable.ic_user)
                .into(friendAvatar);

        // 构建完整的头像URL
        String photoUrl = Constants.USER_PHOTO_BASE_URL + userPhoto;
        Log.d(TAG, "Loading avatar from URL: " + photoUrl);

        // 从服务器下载并缓存最新头像
        cacheManager.cacheAvatar(friendUsername, photoUrl, new AvatarCacheManager.CacheCallback() {
            @Override
            public void onSuccess(String filePath) {
                runOnUiThread(() -> {
                    // 使用新的时间戳加载头像，确保显示最新版本
                    long timestamp = cacheManager.getAvatarTimestamp(friendUsername);
                    Glide.with(FriendInfoActivity.this)
                            .load(new File(filePath))
                            .signature(new ObjectKey(timestamp))
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .into(friendAvatar);
                    Log.d(TAG, "Successfully updated avatar for " + friendUsername);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to update avatar for " + friendUsername, e);
                // 如果下载失败，尝试加载本地缓存
                String cachedAvatarPath = cacheManager.getCachedAvatarPath(friendUsername);
                if (cachedAvatarPath != null && new File(cachedAvatarPath).exists()) {
                    runOnUiThread(() -> {
                        long timestamp = cacheManager.getAvatarTimestamp(friendUsername);
                        Glide.with(FriendInfoActivity.this)
                                .load(new File(cachedAvatarPath))
                                .signature(new ObjectKey(timestamp))
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(friendAvatar);
                    });
                }
            }
        });
    }

    private void showRemarkDialog() {
        EditText input = new EditText(this);
        String currentRemark = friendRemark.getText().toString();
        if (!"设置标签".equals(currentRemark)) {
            input.setText(currentRemark);
        }

        new AlertDialog.Builder(this)
                .setTitle("设置标签")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newRemark = input.getText().toString().trim();
                    updateRemark(newRemark);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteFriendDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除好友")
                .setMessage("确定要删除该好友吗？")
                .setPositiveButton("确定", (dialog, which) -> deleteFriend())
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
                    friendRemark.setText(newRemark.isEmpty() ? "设置标签" : newRemark);
                    Toast.makeText(FriendInfoActivity.this, "标签更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FriendInfoActivity.this, "标签更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(FriendInfoActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to update remark", t);
            }
        });
    }

    private void deleteFriend() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<Map<String, String>> call = apiService.deleteFriend(currentUsername, friendUsername);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    Toast.makeText(FriendInfoActivity.this, message, Toast.LENGTH_SHORT).show();

                    // 发送广播通知 FriendsFragment 刷新好友列表
                    Intent intent = new Intent("ACTION_REFRESH_FRIENDS_LIST");
                    LocalBroadcastManager.getInstance(FriendInfoActivity.this)
                            .sendBroadcast(intent);

                    // 创建一个返回到 MainActivity 的 Intent
                    Intent mainIntent = new Intent(FriendInfoActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(mainIntent);
                    finish(); // 关闭当前页面
                } else {
                    String error = response.body() != null ? response.body().get("error") : "删除好友失败";
                    Toast.makeText(FriendInfoActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(FriendInfoActivity.this, "网络错误，删除好友失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to delete friend", t);
            }
        });
    }
}