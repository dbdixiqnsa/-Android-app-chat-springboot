package com.example.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.chat.model.FriendRequest;
import com.example.chat.navigation.FragmentAdapter;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;
    private int pendingRequestCount = 0; // 未处理的好友申请数量
    private int totalUnreadMessages = 0; // 未读消息总数
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkFriendRequestsRunnable;
    private static final int CHECK_INTERVAL = 5000; // 5秒检查一次

    private BroadcastReceiver friendsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_REFRESH_FRIENDS_LIST".equals(intent.getAction())) {
                // 通知 AFragment 刷新好友列表
                Intent refreshIntent = new Intent("ACTION_REFRESH_CONVERSATIONS");
                LocalBroadcastManager.getInstance(MainActivity.this)
                    .sendBroadcast(refreshIntent);
            }
        }
    };

    private BroadcastReceiver badgeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_UPDATE_BADGE".equals(intent.getAction())) {
                int count = intent.getIntExtra("count", 0);
                updateBadge(count);
            }
        }
    };

    private BroadcastReceiver unreadCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_UNREAD_COUNT".equals(intent.getAction())) {
                updateUnreadMessageCount();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化底部导航和 ViewPager2
        bottomNavigation = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.view_pager);

        FragmentAdapter adapter = new FragmentAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.setCurrentItem(0, false);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.fragment_a) {
                viewPager.setCurrentItem(0, true);
            } else if (itemId == R.id.fragment_b) {
                viewPager.setCurrentItem(1, true);
            } else if (itemId == R.id.fragment_c) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigation.setSelectedItemId(R.id.fragment_a);
                        break;
                    case 1:
                        bottomNavigation.setSelectedItemId(R.id.fragment_b);
                        break;
                    case 2:
                        bottomNavigation.setSelectedItemId(R.id.fragment_c);
                        break;
                }
            }
        });

        // 初始化定时检查任务
        initializePeriodicCheck();

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_REFRESH_FRIENDS_LIST");
        filter.addAction("ACTION_UPDATE_BADGE");
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(badgeUpdateReceiver, filter);

        setupUnreadMessagesBadge();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(unreadCountReceiver, new IntentFilter("UPDATE_UNREAD_COUNT"));
    }

    private void initializePeriodicCheck() {
        checkFriendRequestsRunnable = new Runnable() {
            @Override
            public void run() {
                checkPendingFriendRequests();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 开始定时检查
        handler.post(checkFriendRequestsRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 停止定时检查
        handler.removeCallbacks(checkFriendRequestsRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkFriendRequestsRunnable);
        // 注销所有广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(friendsUpdateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(badgeUpdateReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unreadCountReceiver);
    }

    private void checkPendingFriendRequests() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);

        if (username == null) {
            Toast.makeText(this, "当前用户未登录", Toast.LENGTH_SHORT).show();
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
                    updateBadge(pendingCount);
                } else {
                    pendingRequestCount = 0;
                    updateBadge(0);
                }
            }

            @Override
            public void onFailure(Call<List<FriendRequest>> call, Throwable t) {
                pendingRequestCount = 0;
                updateBadge(0);
            }
        });
    }

    private void checkUnreadMessages() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", null);

        if (username == null) {
            Toast.makeText(this, "当前用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<Map<String, Object>>> call = apiService.getUnreadCounts(username);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> unreadCounts = response.body();
                    totalUnreadMessages = 0;
                    for (Map<String, Object> map : unreadCounts) {
                        int unreadCount = ((Number) map.get("unreadCount")).intValue();
                        totalUnreadMessages += unreadCount;
                    }
                    updateBadge(totalUnreadMessages);
                } else {
                    totalUnreadMessages = 0;
                    updateBadge(0);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                totalUnreadMessages = 0;
                updateBadge(0);
            }
        });
    }

    private void updateBadge(int count) {
        MenuItem menuItem = bottomNavigation.getMenu().findItem(R.id.fragment_c);
        if (menuItem != null) {
            BadgeDrawable badge = bottomNavigation.getOrCreateBadge(menuItem.getItemId());
            if (count > 0) {
                badge.setVisible(true);
                badge.setNumber(count);
            } else {
                badge.setVisible(false);
            }
        }
    }

    private void setupUnreadMessagesBadge() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.fragment_a);
        badge.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        badge.setBadgeTextColor(getResources().getColor(android.R.color.white));
        badge.setVisible(false);

        startUnreadMessagesPolling();
    }

    private void startUnreadMessagesPolling() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUnreadMessageCount();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateUnreadMessageCount() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", null);
        if (username == null) return;

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<Map<String, Object>>> call = apiService.getUnreadCounts(username);

        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call,
                                   Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int totalUnreadCount = 0;
                    for (Map<String, Object> countMap : response.body()) {
                        Object count = countMap.get("unreadCount");
                        if (count instanceof Number) {
                            totalUnreadCount += ((Number) count).intValue();
                        }
                    }

                    BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.fragment_a);

                    if (totalUnreadCount > 0) {
                        badge.setNumber(totalUnreadCount);
                        badge.setVisible(true);
                    } else {
                        badge.setVisible(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
            }
        });
    }
}
