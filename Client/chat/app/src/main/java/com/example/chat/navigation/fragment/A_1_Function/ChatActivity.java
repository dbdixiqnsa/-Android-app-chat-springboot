package com.example.chat.navigation.fragment.A_1_Function;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.MainActivity;
import com.example.chat.R;
import com.example.chat.dto.MessageDTO;
import com.example.chat.model.User;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 聊天界面活动
public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout back_button;
    private ChatAdapter adapter;
    private List<MessageDTO> messageList = new ArrayList<>();

    private EditText inputEditText;
    private ImageView sendButton;

    private String currentUsername;
    private String friendUsername;
    private String currentUserPhoto;
    private String friendUserPhoto;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    private TextView newMessageHint; // 新消息提示
    private TextView topTitle; // 顶部标题栏

    private boolean isFirstLoad = true; // 是否首次加载消息

    private BroadcastReceiver photoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_USER_PHOTO_UPDATED".equals(intent.getAction())) {
                // 更新当前用户头像
                SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                currentUserPhoto = sharedPreferences.getString("userPhotoPath", null);
                adapter.setCurrentUserPhoto(currentUserPhoto);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        back_button = findViewById(R.id.arrow_left_container);

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // 返回上一个界面
            }
        });

        // 获取用户名
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("username", null);
        currentUserPhoto = sharedPreferences.getString("userPhotoPath", null); // 获取本地头像路径

        // 获取好友用户名
        friendUsername = getIntent().getStringExtra("friendUsername");

        if (currentUsername == null || friendUsername == null) {
            Toast.makeText(this, "用户名错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化顶部标题栏
        topTitle = findViewById(R.id.top_title);
        topTitle.setText("聊天"); // 默认文本，可在获取好友信息后更新

        recyclerView = findViewById(R.id.chat_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(messageList, currentUsername, currentUserPhoto, null);
        recyclerView.setAdapter(adapter);

        // 设置重试发送的点击事件
        adapter.setOnRetrySendClickListener(new ChatAdapter.OnRetrySendClickListener() {
            @Override
            public void onRetrySend(MessageDTO message, int position) {
                resendMessage(message, position);
            }
        });

        // 获取好友的头像和昵称
        fetchFriendUserPhoto();

        inputEditText = findViewById(R.id.input_edit_text);
        sendButton = findViewById(R.id.send_button);

        // 发送消息
        sendButton.setOnClickListener(v -> {
            String content = inputEditText.getText().toString().trim();
            if (!content.isEmpty()) {
                sendMessage(content);
                inputEditText.setText("");
            }
        });

        // 初始化新消息提示
        newMessageHint = findViewById(R.id.new_message_hint);
        newMessageHint.setVisibility(View.GONE);
        newMessageHint.setOnClickListener(v -> {
            recyclerView.scrollToPosition(messageList.size() - 1);
            newMessageHint.setVisibility(View.GONE);
        });

        // 当 RecyclerView 滚动时，检测是否滑动到底部，隐藏新消息提示
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (isRecyclerViewAtBottom()) {
                    newMessageHint.setVisibility(View.GONE);
                }
            }
        });

        // 开始轮询更新消息
        startUpdatingMessages();

        // 设置返回按钮
        findViewById(R.id.arrow_left_container).setOnClickListener(v -> finish());

        // 设置更多按钮点击事件
        findViewById(R.id.btn_more).setOnClickListener(v -> navigateToFriendInfo());
    }



    @Override
    protected void onResume() {
        super.onResume();
        // 更新当前用户头像
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserPhoto = sharedPreferences.getString("userPhotoPath", null);
        adapter.setCurrentUserPhoto(currentUserPhoto);

        // 刷新好友列表和好友申请数量
        fetchFriendUserPhoto();
        fetchMessages();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("ACTION_USER_PHOTO_UPDATED");
        LocalBroadcastManager.getInstance(this).registerReceiver(photoUpdateReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(photoUpdateReceiver);
    }

    private void startUpdatingMessages() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMessages();
                handler.postDelayed(this, 2000); // 每2秒更新一次
            }
        };
        handler.post(updateRunnable);
    }

    //获取好友的头像和昵称
    private void fetchFriendUserPhoto() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Map<String, String> payload = new HashMap<>();
        payload.put("username", friendUsername);

        Call<User> call = apiService.getUserDetails(payload);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User friend = response.body();
                    friendUserPhoto = friend.getUserPhoto();

                    // 更新 Adapter 中的好友头像
                    adapter.setFriendUserPhoto(friendUserPhoto);

                    // 设置顶部标题栏为好友的昵称
                    topTitle.setText(friend.getNickname());
                } else {
                    Toast.makeText(ChatActivity.this, "无法获取好友信息", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //获取消息列表
    private void fetchMessages() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Map<String, String> payload = new HashMap<>();
        payload.put("senderUsername", currentUsername);
        payload.put("receiverUsername", friendUsername);

        Call<List<MessageDTO>> call = apiService.getMessages(payload);
        call.enqueue(new Callback<List<MessageDTO>>() {
            @Override
            public void onResponse(Call<List<MessageDTO>> call, Response<List<MessageDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MessageDTO> newMessages = response.body();

                    // 使用 DiffUtil 计算差异
                    ChatDiffCallback diffCallback = new ChatDiffCallback(new ArrayList<>(messageList), newMessages);
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

                    // 更新消息列表
                    messageList.clear();
                    messageList.addAll(newMessages);

                    // 通知适配器更新
                    diffResult.dispatchUpdatesTo(adapter);

                    if (isFirstLoad) {
                        // 首次加载，滚动到最新消息
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        isFirstLoad = false;
                    } else {
                        if (diffCallback.hasNewMessages()) {
                            // 有新消息，滚动到底部
                            recyclerView.scrollToPosition(messageList.size() - 1);
                            newMessageHint.setVisibility(View.GONE);
                        }
                        // 没有新消息，保持用户当前位置
                    }

                    // 标记消息为已读
                    markMessagesAsRead();

                } else {
                    Toast.makeText(ChatActivity.this, "无法获取消息", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MessageDTO>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //标记消息为已读
    private void markMessagesAsRead() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Map<String, String> payload = new HashMap<>();
        payload.put("senderUsername", friendUsername);
        payload.put("receiverUsername", currentUsername);

        Call<Void> call = apiService.markMessagesAsRead(payload);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    //发送消息
    private void sendMessage(String content) {
        // 创建消息对象
        MessageDTO message = new MessageDTO();
        message.setSenderUsername(currentUsername);
        message.setReceiverUsername(friendUsername);
        message.setContent(content);
        message.setTimestamp(getCurrentTimestamp());
        // 将消息添加到列表并更新界面
        messageList.add(message);
        int messageIndex = messageList.size() - 1;
        adapter.notifyItemInserted(messageIndex);
        recyclerView.scrollToPosition(messageIndex);
        // 发送消息到服务器
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<MessageDTO> call = apiService.sendMessage(message);
        call.enqueue(new Callback<MessageDTO>() {
            @Override
            public void onResponse(Call<MessageDTO> call, Response<MessageDTO> response) {
                if (!response.isSuccessful()) {
                    // 发送失败，更新消息状态并提示
                    message.setFailed(true);
                    adapter.notifyItemChanged(messageIndex);
                    Toast.makeText(ChatActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                } else {
                    // 发送成功，更新消息
                    MessageDTO sentMessage = response.body();
                    if (sentMessage != null) {
                        messageList.set(messageIndex, sentMessage);
                        adapter.notifyItemChanged(messageIndex);
                    }
                }
            }

            @Override
            public void onFailure(Call<MessageDTO> call, Throwable t) {
                // 发送失败，更新消息状态并提示
                message.setFailed(true);
                adapter.notifyItemChanged(messageIndex);
                Toast.makeText(ChatActivity.this, "网络错误，发送失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //重发消息
    private void resendMessage(MessageDTO message, int position) {
        // 重置消息的失败状态
        message.setFailed(false);
        adapter.notifyItemChanged(position);

        // 发送消息到服务器
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<MessageDTO> call = apiService.sendMessage(message);

        call.enqueue(new Callback<MessageDTO>() {
            @Override
            public void onResponse(Call<MessageDTO> call, Response<MessageDTO> response) {
                if (!response.isSuccessful()) {
                    // 发送失败，更新状态
                    message.setFailed(true);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(ChatActivity.this, "重发失败", Toast.LENGTH_SHORT).show();
                } else {
                    // 发送成功，更新消息
                    MessageDTO sentMessage = response.body();
                    if (sentMessage != null) {
                        messageList.set(position, sentMessage);
                        adapter.notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onFailure(Call<MessageDTO> call, Throwable t) {
                // 发送失败，更新状态
                message.setFailed(true);
                adapter.notifyItemChanged(position);
                Toast.makeText(ChatActivity.this, "网络错误，重发失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //获取当前时间的时间戳字符串
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    //检查RecyclerView是否滚动到最底部
    private boolean isRecyclerViewAtBottom() {
        if (recyclerView == null || recyclerView.getAdapter() == null || recyclerView.getLayoutManager() == null) {
            return false;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
        int itemCount = recyclerView.getAdapter().getItemCount();
        return lastVisibleItemPosition >= itemCount - 1;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent intent = new Intent("UPDATE_UNREAD_COUNT");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
    }

    // 跳转到好友信息界面
    private void navigateToFriendInfo() {
        Intent intent = new Intent(this, FriendInfoActivity.class);
        intent.putExtra("friendUsername", friendUsername);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // 返回到 MainActivity
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
