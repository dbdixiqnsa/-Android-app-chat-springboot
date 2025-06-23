package com.example.chat.navigation.fragment.C_2_Function;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.model.FriendRequest;
import com.example.chat.model.User;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendsFragment extends Fragment {

    private RecyclerView friendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private List<User> friendsList;
    private List<FriendListItem> friendListItems;
    private View addFriendButton;
    private View addFriendBadge;
    private LinearLayout back;
    private PopupWindow popupWindow;

    private SideBar sideBar;
    private TextView overlay;

    private static final int REQUEST_CODE_FRIEND_REQUESTS = 1;

    private int pendingRequestCount = 0;

    private String userPhotoPath;

    // 定义广播接收器
    private BroadcastReceiver photoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_USER_PHOTO_UPDATED".equals(intent.getAction())) {
                // 获取新的头像路径
                userPhotoPath = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .getString("userPhotoPath", null);
                // 更新适配器
                if (friendsAdapter != null) {
                    friendsAdapter.updateUserPhotoPath(userPhotoPath);
                }
            }
        }
    };

    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_REFRESH_FRIENDS_LIST".equals(intent.getAction())) {
                getFriendsList(); // 刷新好友列表
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends_list, container, false);

        back = view.findViewById(R.id.arrow_left_container);
        back.setOnClickListener(v -> requireActivity().finish());

        friendsRecyclerView = view.findViewById(R.id.friends_recycler_view);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsList = new ArrayList<>();
        friendListItems = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friendListItems, getContext(), userPhotoPath);

        // 设置长按监听删除好友
        friendsAdapter.setOnFriendLongClickListener((user, position) -> showDeleteConfirmationDialog(user, position));

        friendsRecyclerView.setAdapter(friendsAdapter);

        addFriendButton = view.findViewById(R.id.add_friend_button);
        addFriendBadge = view.findViewById(R.id.add_friend_badge);

        addFriendButton.setOnClickListener(this::showAddFriendMenu);

        sideBar = view.findViewById(R.id.side_bar);
        overlay = view.findViewById(R.id.overlay);
        sideBar.setOverlay(overlay);

        sideBar.setOnTouchingLetterChangedListener(s -> {
            int position = getPositionForSection(s.charAt(0));
            if (position != -1) {
                ((LinearLayoutManager) friendsRecyclerView.getLayoutManager())
                        .scrollToPositionWithOffset(position, 0);
            }
        });
        IntentFilter filter = new IntentFilter("ACTION_USER_PHOTO_UPDATED");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(photoUpdateReceiver, filter);
        getFriendsList();
        checkPendingFriendRequests();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(photoUpdateReceiver);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter("ACTION_REFRESH_FRIENDS_LIST");
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(refreshReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(refreshReceiver);
    }

    private int getPositionForSection(char section) {
        for (int i = 0; i < friendListItems.size(); i++) {
            FriendListItem item = friendListItems.get(i);
            if (item.getType() == FriendListItem.TYPE_HEADER) {
                if (item.getHeader().charAt(0) == section) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPendingFriendRequests();
        getFriendsList();
    }

    private void getFriendsList() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("username", null);

        if (username == null) {
            Toast.makeText(getContext(), "当前用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<User>> call = apiService.getFriendsList(username);
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> users = response.body();
                    Gson gson = new Gson();

                    friendsList.clear();
                    friendsList.addAll(users);

                    friendListItems.clear();
                    String lastLetter = null;

                    // 处理生成 FriendItem 列表
                    List<FriendItem> friendItems = new ArrayList<>();
                    for (User user : friendsList) {
                        friendItems.add(new FriendItem(user));
                    }

                    // 对 friendItems 进行排序
                    Collections.sort(friendItems, (o1, o2) -> {
                        String letter1 = o1.getInitialLetter();
                        String letter2 = o2.getInitialLetter();

                        // 如果有任何一个是 null，给它一个默认值
                        if (letter1 == null) letter1 = "#";
                        if (letter2 == null) letter2 = "#";

                        // 如果都是 #，按照昵称排序
                        if (letter1.equals("#") && letter2.equals("#")) {
                            return o1.getUser().getNickname().compareTo(o2.getUser().getNickname());
                        }
                        // 如果只有一个是 #，将其排在最后
                        else if (letter1.equals("#")) {
                            return 1;
                        }
                        else if (letter2.equals("#")) {
                            return -1;
                        }
                        // 否则按字母顺序排序
                        else {
                            int letterCompare = letter1.compareTo(letter2);
                            if (letterCompare == 0) {
                                // 如果首字母相同，按昵称排序
                                return o1.getUser().getNickname().compareTo(o2.getUser().getNickname());
                            }
                            return letterCompare;
                        }
                    });

                    // 添加分组和项目
                    for (FriendItem item : friendItems) {
                        String currentLetter = item.getInitialLetter();
                        // 只有当字母变化时才添加新的头部
                        if (!currentLetter.equals(lastLetter)) {
                            // 只添加A-Z和#的头部
                            if (currentLetter.matches("[A-Z#]")) {
                                friendListItems.add(new FriendListItem(FriendListItem.TYPE_HEADER, currentLetter, null));
                                lastLetter = currentLetter;
                            }
                        }
                        // 添加好友项
                        friendListItems.add(new FriendListItem(FriendListItem.TYPE_ITEM, null, item.getUser()));
                    }

                    // 更新适配器
                    friendsAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "获取好友列表失败: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(getContext(), "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPendingFriendRequests() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String username = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
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
                    // 统计状态为 "PENDING" 的好友申请
                    int pendingCount = 0;
                    for (FriendRequest request : requests) {
                        // 添加 null 检查
                        if (request != null && request.getStatus() != null && "PENDING".equals(request.getStatus())) {
                            pendingCount++;
                        }
                    }
                    pendingRequestCount = pendingCount;
                    updateAddFriendBadge();
                } else {
                    pendingRequestCount = 0;
                    updateAddFriendBadge();
                }
            }

            @Override
            public void onFailure(Call<List<FriendRequest>> call, Throwable t) {
                pendingRequestCount = 0;
                updateAddFriendBadge();
            }
        });
    }

    private void updateAddFriendBadge() {
        if (addFriendBadge != null) {
            if (pendingRequestCount > 0) {
                addFriendBadge.setVisibility(View.VISIBLE);
                // 如果是 TextView，可以设置文本为 pendingRequestCount
                if (addFriendBadge instanceof TextView) {
                    ((TextView) addFriendBadge).setText(String.valueOf(pendingRequestCount));
                }
            } else {
                addFriendBadge.setVisibility(View.GONE);
            }
        }
    }

    private void showDeleteConfirmationDialog(User user, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除好友")
                .setMessage("确定要删除好友 " + user.getNickname() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteFriend(user, position))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteFriend(User user, int position) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // 从 SharedPreferences 获取当前用户名
        String currentUsername = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("username", null);
        // 从 User 对象获取好友的用户名
        String friendUsername = user.getUsername();

        if (currentUsername == null) {
            Toast.makeText(getContext(), "当前用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 发送请求，使用实际的用户名和好友用户名作为查询参数
        Call<Map<String, String>> call = apiService.deleteFriend(currentUsername, friendUsername);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    // 从列表中移除已删除的好友
                    friendListItems.remove(position);
                    friendsAdapter.notifyItemRemoved(position);
                } else {
                    String error = response.body() != null ? response.body().get("error") : "删除好友失败";
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误，删除好友失败", Toast.LENGTH_SHORT).show();
            }
        });
    }


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
                startActivityForResult(friendRequestsIntent, REQUEST_CODE_FRIEND_REQUESTS);
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
}
