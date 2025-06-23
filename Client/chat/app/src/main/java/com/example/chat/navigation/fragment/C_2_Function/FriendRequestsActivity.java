package com.example.chat.navigation.fragment.C_2_Function;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.model.FriendRequest;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRequestsActivity extends AppCompatActivity {

    private RecyclerView friendRequestsRecyclerView;
    private FriendRequestsAdapter requestsAdapter;
    private List<FriendRequest> friendRequests;
    private View arrowLeftContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        Toolbar toolbar = findViewById(R.id.toolbar);

        // 获取返回按钮容器并设置点击事件
        arrowLeftContainer = toolbar.findViewById(R.id.arrow_left_container);
        arrowLeftContainer.setOnClickListener(v -> {
            // 执行返回操作
            onBackPressed();
        });

        friendRequestsRecyclerView = findViewById(R.id.friend_requests_recycler_view);
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendRequests = new ArrayList<>();
        requestsAdapter = new FriendRequestsAdapter(friendRequests,
                this::acceptFriendRequest,
                this::declineFriendRequest);
        friendRequestsRecyclerView.setAdapter(requestsAdapter);

        getFriendRequests();
    }

    private void getFriendRequests() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);

        Call<List<FriendRequest>> call = apiService.getFriendRequests(username);
        call.enqueue(new Callback<List<FriendRequest>>() {
            @Override
            public void onResponse(Call<List<FriendRequest>> call, Response<List<FriendRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRequest> requests = response.body();

                    // 按照最新的请求在顶部排序（id 越大越新）
                    Collections.sort(requests, new Comparator<FriendRequest>() {
                        @Override
                        public int compare(FriendRequest o1, FriendRequest o2) {
                            return o2.getId().compareTo(o1.getId());
                        }
                    });

                    friendRequests.clear();
                    friendRequests.addAll(requests);
                    requestsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<FriendRequest>> call, Throwable t) {
            }
        });
    }


    private void acceptFriendRequest(Long requestId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Map<String, Long> requestPayload = new HashMap<>();
        requestPayload.put("requestId", requestId);

        Call<Map<String, String>> call = apiService.acceptFriendRequest(requestPayload);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendRequestsActivity.this, "好友申请已同意", Toast.LENGTH_SHORT).show();
                    getFriendRequests(); // 更新好友请求列表

                    // 发送广播通知刷新好友列表
                    Intent refreshIntent = new Intent("ACTION_REFRESH_FRIENDS_LIST");
                    LocalBroadcastManager.getInstance(FriendRequestsActivity.this)
                        .sendBroadcast(refreshIntent);

                    // 设置结果为 RESULT_OK，通知前一个界面刷新
                    setResult(Activity.RESULT_OK);
                } else {
                    Toast.makeText(FriendRequestsActivity.this, "操作失败: " + response.message(), 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(FriendRequestsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void declineFriendRequest(Long requestId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Map<String, Long> requestPayload = new HashMap<>();
        requestPayload.put("requestId", requestId);

        Call<Map<String, String>> call = apiService.declineFriendRequest(requestPayload);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FriendRequestsActivity.this, "好友申请已拒绝", Toast.LENGTH_SHORT).show();
                    getFriendRequests(); // 更新好友请求列表
                    // 设置结果为 RESULT_OK，通知前一个界面刷新
                    setResult(Activity.RESULT_OK);
                } else {
                    Toast.makeText(FriendRequestsActivity.this, "操作失败: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(FriendRequestsActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
