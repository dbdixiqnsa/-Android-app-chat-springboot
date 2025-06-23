package com.example.chat.navigation.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.model.FriendConversation;
import com.example.chat.model.FriendRequest;
import com.example.chat.model.User;
import com.example.chat.navigation.fragment.A_1_Function.ChatActivity;
import com.example.chat.navigation.fragment.A_1_Function.ConversationAdapter;
import com.example.chat.navigation.fragment.C_2_Function.AddFriendActivity;
import com.example.chat.navigation.fragment.C_2_Function.CustomMenuAdapter;
import com.example.chat.navigation.fragment.C_2_Function.FriendRequestsActivity;
import com.example.chat.navigation.fragment.C_2_Function.MenuItem;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AFragment extends Fragment {

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private String currentUsername;

    private ImageView addFriendButton;
    private View addFriendBadge;

    private PopupWindow popupWindow;
    private int pendingRequestCount = 0; // 未处理的好友申请数量

    private List<FriendConversation> conversationList = new ArrayList<>();

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkRequestsRunnable;
    private static final int CHECK_INTERVAL = 5000;

    // 定义广播接收器
    private BroadcastReceiver photoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_USER_PHOTO_UPDATED".equals(intent.getAction())) {
                // 重新获取好友列表以更新头像
                fetchFriendList();
            }
        }
    };

    private BroadcastReceiver conversationsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("ACTION_REFRESH_CONVERSATIONS".equals(action)) {
                fetchFriendList();
            } else if ("ACTION_NEW_FRIEND_REQUEST".equals(action)) {
                checkPendingFriendRequests();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_a, container, false);

        // 初始化添加好友按钮和红点
        addFriendButton = view.findViewById(R.id.add_friend_button);
        addFriendBadge = view.findViewById(R.id.add_friend_badge);

        // 设置点击事件
        addFriendButton.setOnClickListener(this::showAddFriendMenu);

        recyclerView = view.findViewById(R.id.conversation_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 获取当前用户名
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("username", null);

        if (currentUsername == null) {
            Toast.makeText(getContext(), "用户未登录", Toast.LENGTH_SHORT).show();
            return view;
        }

        adapter = new ConversationAdapter(conversationList, currentUsername);
        recyclerView.setAdapter(adapter);

        // 设置点击事件，点击进入聊天界面
        adapter.setOnItemClickListener((position, conversation) -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("friendUsername", conversation.getFriend().getUsername());
            startActivity(intent);
        });

        // 获取好友列表
        fetchFriendList();

        // 检查未处理的好友申请数量
        checkPendingFriendRequests();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_REFRESH_CONVERSATIONS");
        filter.addAction("ACTION_NEW_FRIEND_REQUEST");
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(conversationsUpdateReceiver, filter);

        // 初始化定时检查任务
        initializePeriodicCheck();
    }

    private void initializePeriodicCheck() {
        checkRequestsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) { // 确保Fragment还附加在Activity上
                    checkPendingFriendRequests();
                    fetchFriendList();
                    handler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("ACTION_USER_PHOTO_UPDATED");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(photoUpdateReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // 注销广播接收器
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(photoUpdateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 开始定时检查
        handler.post(checkRequestsRunnable);
        fetchFriendList();
        checkPendingFriendRequests();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 停止定时检查
        handler.removeCallbacks(checkRequestsRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(conversationsUpdateReceiver);
        // 移除所有回调
        handler.removeCallbacks(checkRequestsRunnable);
    }

    // 检查未处理的好友申请数量
    private void checkPendingFriendRequests() {
        if (!isAdded()) return; // 确保Fragment还附加在Activity上

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("username", null);

        if (username == null) return;

        Call<List<FriendRequest>> call = apiService.getFriendRequests(username);
        call.enqueue(new Callback<List<FriendRequest>>() {
            @Override
            public void onResponse(Call<List<FriendRequest>> call, Response<List<FriendRequest>> response) {
                if (!isAdded()) return; // 再次检查Fragment是否还附加在Activity上

                if (response.isSuccessful() && response.body() != null) {
                    List<FriendRequest> requests = response.body();
                    int pendingCount = 0;
                    for (FriendRequest request : requests) {
                        if ("PENDING".equals(request.getStatus())) {
                            pendingCount++;
                        }
                    }
                    pendingRequestCount = pendingCount;
                    updateAddFriendBadge();

                    // 如果有新的好友申请，发送广播通知MainActivity更新底部导航栏的红点
                    if (pendingCount > 0) {
                        Intent intent = new Intent("ACTION_UPDATE_BADGE");
                        intent.putExtra("count", pendingCount);
                        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<FriendRequest>> call, Throwable t) {
                if (!isAdded()) return;
                pendingRequestCount = 0;
                updateAddFriendBadge();
            }
        });
    }

    private void updateAddFriendBadge() {
        if (addFriendBadge != null) {
            if (pendingRequestCount > 0) {
                addFriendBadge.setVisibility(View.VISIBLE);
            } else {
                addFriendBadge.setVisibility(View.GONE);
            }
        }
    }

    // 显示添加好友菜单
    private void showAddFriendMenu(View anchorView) {
        // 加载自定义的弹出菜单布局
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.custom_popup_menu, null);
        RecyclerView menuRecyclerView = popupView.findViewById(R.id.menu_recycler_view);
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 创建菜单项
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem(R.drawable.ic_add_friend, "添加好友", 0));
        menuItems.add(new MenuItem(R.drawable.ic_friend_requests, "好友申请", pendingRequestCount));

        // 创建适配器
        CustomMenuAdapter menuAdapter = new CustomMenuAdapter(getContext(), menuItems, position -> {
            // 处理菜单项点击事件
            if (position == 0) {
                // 添加好友
                Intent addFriendIntent = new Intent(getActivity(), AddFriendActivity.class);
                startActivity(addFriendIntent);
            } else if (position == 1) {
                // 查看好友申请
                Intent friendRequestsIntent = new Intent(getActivity(), FriendRequestsActivity.class);
                startActivity(friendRequestsIntent);
            }
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        });
        menuRecyclerView.setAdapter(menuAdapter);

        // 创建 PopupWindow
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 显示 PopupWindow
        popupWindow.showAsDropDown(anchorView, 0, 10); // 调整偏移量以确保显示正常
    }

    // 接收广播后重新获取好友列表
    private void fetchFriendList() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<Map<String, Object>>> call = apiService.getFriendsWithLatestMessage(currentUsername);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> data = response.body();
                    conversationList.clear();

                    // 使用 Gson 解析返回的数据
                    Gson gson = new Gson();

                    // 创建一个临时的好友用户名列表
                    List<String> friendUsernames = new ArrayList<>();

                    for (Map<String, Object> item : data) {
                        // 解析 friend 对象
                        String friendJson = gson.toJson(item.get("friend"));
                        User friend = gson.fromJson(friendJson, User.class);

                        // 获取最新消息和时间戳
                        String latestMessage = (String) item.get("latestMessage");
                        String timestamp = (String) item.get("timestamp");

                        // 先创建 FriendConversation 对象，未设置 unreadCount
                        FriendConversation conversation = new FriendConversation(friend, 0); // 未读消息数量先设为0
                        conversation.setLatestMessage(latestMessage);
                        conversation.setTimestamp(timestamp);
                        conversationList.add(conversation);

                        // 添加好友用户名到列表
                        friendUsernames.add(friend.getUsername());
                    }

                    // 获取未读消息数量
                    fetchUnreadCounts(friendUsernames);
                } else {
                    Toast.makeText(getContext(), "无法获取好友列表", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUnreadCounts(List<String> friendUsernames) {
        if (friendUsernames.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<Map<String, Object>>> call = apiService.getUnreadCounts(currentUsername);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> unreadCounts = response.body();
                    Map<String, Integer> unreadCountsMap = new HashMap<>();
                    for (Map<String, Object> map : unreadCounts) {
                        String senderUsername = (String) map.get("senderUsername");
                        int unreadCount = ((Number) map.get("unreadCount")).intValue();
                        unreadCountsMap.put(senderUsername, unreadCount);
                    }

                    // 更新 conversationList 中的 unreadCount
                    for (FriendConversation conversation : conversationList) {
                        String friendUsername = conversation.getFriend().getUsername();
                        int unreadCount = unreadCountsMap.getOrDefault(friendUsername, 0);
                        conversation.setUnreadCount(unreadCount);
                    }

                    // 通知适配器数据已更新
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "无法获取未读消息数量", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
