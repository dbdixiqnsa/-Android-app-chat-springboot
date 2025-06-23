package com.example.chat.navigation.fragment.A_1_Function;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.R;
import com.example.chat.dto.MessageDTO;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<MessageDTO> messageList;
    private String currentUsername;
    private String currentUserPhoto;
    private String friendUserPhoto;

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    // 定义重试发送的点击事件接口
    public interface OnRetrySendClickListener {
        void onRetrySend(MessageDTO message, int position);
    }

    private OnRetrySendClickListener onRetrySendClickListener;

    public void setOnRetrySendClickListener(OnRetrySendClickListener listener) {
        this.onRetrySendClickListener = listener;
    }

    public ChatAdapter(List<MessageDTO> messageList, String currentUsername, String currentUserPhoto, String friendUserPhoto) {
        this.messageList = messageList;
        this.currentUsername = currentUsername;
        this.currentUserPhoto = currentUserPhoto;
        this.friendUserPhoto = friendUserPhoto;
    }

    /**
      更新当前用户的头像路径，并刷新界面
      @param currentUserPhoto 当前用户的头像路径
    */
    public void setCurrentUserPhoto(String currentUserPhoto) {
        this.currentUserPhoto = currentUserPhoto;
        notifyDataSetChanged();
    }

    /**
      更新好友的头像路径，并刷新界面
      @param friendUserPhoto 好友的头像路径
     */
    public void setFriendUserPhoto(String friendUserPhoto) {
        this.friendUserPhoto = friendUserPhoto;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        MessageDTO message = messageList.get(position);
        if (message.getSenderUsername().equals(currentUsername)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageHolder(view);
        } else { // TYPE_RECEIVED
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MessageDTO message = messageList.get(position);
        AvatarCacheManager cacheManager = new AvatarCacheManager(holder.itemView.getContext());

        // 判断是否显示时间戳
        boolean showTimestamp = false;
        if (position == 0) {
            showTimestamp = true;
        } else {
            MessageDTO previousMessage = messageList.get(position - 1);
            Date currentTime = parseTimestamp(message.getTimestamp());
            Date previousTime = parseTimestamp(previousMessage.getTimestamp());
            long diff = currentTime.getTime() - previousTime.getTime();
            if (diff >= TIME_GAP) {
                showTimestamp = true;
            }
        }

        if (holder instanceof SentMessageHolder) {
            SentMessageHolder sentHolder = (SentMessageHolder) holder;
            sentHolder.messageTextView.setText(message.getContent());

            if (showTimestamp) {
                sentHolder.timestampTextView.setText(formatTimestamp(message.getTimestamp()));
                sentHolder.timestampTextView.setVisibility(View.VISIBLE);
            } else {
                sentHolder.timestampTextView.setVisibility(View.GONE);
            }

            // 加载当前用户的头像
            if (currentUserPhoto != null && !currentUserPhoto.isEmpty() && new File(currentUserPhoto).exists()) {
                // 使用时间戳作为 signature
                long timestamp = cacheManager.getAvatarTimestamp(currentUsername);
                Glide.with(holder.itemView.getContext())
                        .load(new File(currentUserPhoto))
                        .signature(new ObjectKey(timestamp))
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(sentHolder.avatarImageView);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.ic_user)
                        .into(sentHolder.avatarImageView);
            }

            if (message.isFailed()) {
                sentHolder.messageTextView.setTextColor(Color.RED);
                sentHolder.itemView.setOnClickListener(v -> {
                    if (onRetrySendClickListener != null) {
                        onRetrySendClickListener.onRetrySend(message, position);
                    }
                });
            } else {
                sentHolder.messageTextView.setTextColor(Color.BLACK);
                sentHolder.itemView.setOnClickListener(null);
            }

        } else if (holder instanceof ReceivedMessageHolder) {
            ReceivedMessageHolder receivedHolder = (ReceivedMessageHolder) holder;
            receivedHolder.messageTextView.setText(message.getContent());

            if (showTimestamp) {
                receivedHolder.timestampTextView.setText(formatTimestamp(message.getTimestamp()));
                receivedHolder.timestampTextView.setVisibility(View.VISIBLE);
            } else {
                receivedHolder.timestampTextView.setVisibility(View.GONE);
            }

            // 使用缓存的好友头像
            String cachedAvatarPath = cacheManager.getCachedAvatarPath(message.getSenderUsername());
            if (cachedAvatarPath != null && new File(cachedAvatarPath).exists()) {
                // 使用时间戳作为 signature
                long timestamp = cacheManager.getAvatarTimestamp(message.getSenderUsername());
                Glide.with(holder.itemView.getContext())
                        .load(new File(cachedAvatarPath))
                        .signature(new ObjectKey(timestamp))
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(receivedHolder.avatarImageView);
            } else {
                // 如果缓存中没有，则使用默认头像并尝试重新缓存
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.ic_user)
                        .into(receivedHolder.avatarImageView);

                // 后台重新缓存头像
                String photoUrl = Constants.USER_PHOTO_BASE_URL + friendUserPhoto;
                cacheManager.cacheAvatar(message.getSenderUsername(), photoUrl,
                        new AvatarCacheManager.CacheCallback() {
                            @Override
                            public void onSuccess(String filePath) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    // 使用时间戳作为 signature
                                    long timestamp = cacheManager.getAvatarTimestamp(message.getSenderUsername());
                                    Glide.with(holder.itemView.getContext())
                                            .load(new File(filePath))
                                            .signature(new ObjectKey(timestamp))
                                            .placeholder(R.drawable.ic_user)
                                            .error(R.drawable.ic_user)
                                            .into(receivedHolder.avatarImageView);
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("ChatAdapter", "Failed to cache avatar for " +
                                        message.getSenderUsername(), e);
                            }
                        });
            }
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView avatarImageView;
        TextView timestampTextView;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.sent_message_text);
            avatarImageView = itemView.findViewById(R.id.avatar_image_view);
            timestampTextView = itemView.findViewById(R.id.sent_message_timestamp);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView avatarImageView;
        TextView timestampTextView;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.received_message_text);
            avatarImageView = itemView.findViewById(R.id.avatar_image_view);
            timestampTextView = itemView.findViewById(R.id.received_message_timestamp);

            // 为头像添加点击事件
            avatarImageView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), FriendInfoActivity.class);
                intent.putExtra("friendUsername", messageList.get(getAdapterPosition()).getSenderUsername());
                itemView.getContext().startActivity(intent);
            });
        }
    }

    // 时间间隔，10分钟
    private static final long TIME_GAP = 10 * 60 * 1000;

    // 解析时间戳
    private Date parseTimestamp(String timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        try {
            return sdf.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date(); // 解析失败，返回当前时间
        }
    }

    // 格式化时间戳用于显示
    private String formatTimestamp(String timestamp) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return timestamp; // 解析失败，返回原始字符串
        }
    }
}
