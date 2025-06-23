package com.example.chat.navigation.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.InformationActivity;
import com.example.chat.R;
import com.example.chat.model.FriendRequest;
import com.example.chat.navigation.fragment.C_2_Function.FriendsActivity;
import com.example.chat.navigation.fragment.C_3_Function.PrivacyActivity;
import com.example.chat.navigation.fragment.C_4_Function.SettingsActivity;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.example.chat.utils.Constants;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CFragment extends Fragment {

    private TextView nicknameTextView;
    private TextView usernameTextView;
    private ImageView userImageView;
    private View friendsBadge; // 好友项的红点
    private int pendingRequestCount = 0; // 未处理的好友申请数量

    private static final int REQUEST_CODE_FRIENDS = 1001;

    // 定义广播接收器
    private BroadcastReceiver photoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_USER_PHOTO_UPDATED".equals(intent.getAction())) {
                // 重新更新用户信息
                updateUserInfo();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_c, container, false);

        // 获取顶部用户信息部分
        RelativeLayout userInfoSection = view.findViewById(R.id.user_info_section);
        RelativeLayout privacyButton = view.findViewById(R.id.privacy_button);
        RelativeLayout friendsButton = view.findViewById(R.id.friends_button);
        RelativeLayout settingsButton = view.findViewById(R.id.settings_button);

        nicknameTextView = view.findViewById(R.id.nickname_text);
        usernameTextView = view.findViewById(R.id.username);
        userImageView = view.findViewById(R.id.image_user);
        friendsBadge = view.findViewById(R.id.friends_badge);

        // 更新用户信息
        updateUserInfo();

        // 设置点击事件，跳转到 InformationActivity
        userInfoSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), InformationActivity.class);
            startActivity(intent);
        });

        privacyButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PrivacyActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        // 设置点击事件，跳转到好友界面
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FriendsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_FRIENDS);
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("ACTION_USER_PHOTO_UPDATED");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(photoUpdateReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销广播接收器，避免内存泄漏
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(photoUpdateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 在 Fragment 重新可见时更新用户信息
        updateUserInfo();
        // 检查未处理的好友申请
        checkPendingFriendRequests();
    }

    private void updateUserInfo() {
        // 从 SharedPreferences 中获取最新的 nickname、username 和 userPhotoPath
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        String nickname = sharedPreferences.getString("nickname", "默认名字");
        String username = sharedPreferences.getString("username", "默认账号");
        String userPhotoPath = sharedPreferences.getString("userPhotoPath", null);

        // 显示用户名和昵称
        if (nicknameTextView != null) {
            nicknameTextView.setText(nickname);
        }
        if (usernameTextView != null) {
            usernameTextView.setText(username);
        }

        if (userPhotoPath != null) {
            File photoFile = new File(userPhotoPath);
            if (photoFile.exists()) {
                // 如果 SharedPreferences 中的 userPhotoPath 存在且文件存在，则加载该文件
                Glide.with(this)
                        .load(photoFile)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .signature(new ObjectKey(photoFile.length() + "_" + photoFile.lastModified()))
                        .into(userImageView);
                return;
            }
        }

        // 如果没有 userPhotoPath 或文件不存在，尝试加载外部文件
        File photoFile = new File(getActivity().getExternalFilesDir(null), "user_photo.jpg");
        if (photoFile.exists()) {
            // 如果外部文件存在，则加载
            Glide.with(this)
                    .load(photoFile)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .signature(new ObjectKey(photoFile.length() + "_" + photoFile.lastModified()))
                    .into(userImageView);
        } else {
            // 如果本地文件不存在，加载默认头像或服务器头像
            Glide.with(this)
                    .load(Constants.USER_PHOTO_BASE_URL + "default.jpg")
                    .placeholder(R.drawable.ic_user)
                    .into(userImageView);
        }
    }

    private void checkPendingFriendRequests() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE)
                .getString("username", null);

        if (username == null) {
            Toast.makeText(getContext(), "当前用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<FriendRequest>> call = apiService.getFriendRequests(username);
        call.enqueue(new Callback<List<FriendRequest>>() {
            @Override
            public void onResponse(Call<List<FriendRequest>> call, Response<List<FriendRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRequest> requests = response.body();
                    // 仅统计状态为 "PENDING" 的好友申请
                    int pendingCount = 0;
                    for (FriendRequest request : requests) {
                        if ("PENDING".equals(request.getStatus())) {
                            pendingCount++;
                        }
                    }
                    pendingRequestCount = pendingCount;
                    updateFriendsBadge();
                } else {
                    pendingRequestCount = 0;
                    updateFriendsBadge();
                }
            }

            @Override
            public void onFailure(Call<List<FriendRequest>> call, Throwable t) {
                pendingRequestCount = 0;
                updateFriendsBadge();
            }
        });
    }

    private void updateFriendsBadge() {
        if (pendingRequestCount > 0) {
            friendsBadge.setVisibility(View.VISIBLE);
        } else {
            friendsBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FRIENDS) {
            // 处理好友页面返回后，重新检查未处理的好友申请
            checkPendingFriendRequests();
        }
    }
}
