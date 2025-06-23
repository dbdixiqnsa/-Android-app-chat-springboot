package com.example.chat.navigation.fragment.A_1_Function;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.R;
import com.example.chat.model.FriendConversation;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<FriendConversation> conversations;
    private String currentUsername;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private AvatarCacheManager cacheManager;

    public interface OnItemClickListener {
        void onItemClick(int position, FriendConversation conversation);
    }

    public ConversationAdapter(List<FriendConversation> conversations, String currentUsername) {
        this.conversations = conversations;
        this.currentUsername = currentUsername;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
            cacheManager = new AvatarCacheManager(context);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendConversation conversation = conversations.get(position);
        String friendUsername = conversation.getFriend().getUsername();
        String friendPhoto = conversation.getFriend().getUserPhoto();

        // 设置基本信息
        holder.nicknameText.setText(conversation.getFriend().getNickname());
        holder.latestMessageText.setText(conversation.getLatestMessage());
        holder.timestampText.setText(formatTime(conversation.getTimestamp()));

        // 设置未读消息数量
        if (conversation.getUnreadCount() > 0) {
            holder.unreadCountText.setVisibility(View.VISIBLE);
            holder.unreadCountText.setText(String.valueOf(conversation.getUnreadCount()));
        } else {
            holder.unreadCountText.setVisibility(View.GONE);
        }

        // 加载头像
        String cachedAvatarPath = cacheManager.getCachedAvatarPath(friendUsername);
        if (cachedAvatarPath != null && new File(cachedAvatarPath).exists()) {
            // 使用时间戳作为 signature，避免频繁刷新
            long timestamp = cacheManager.getAvatarTimestamp(friendUsername);
            Glide.with(context)
                    .load(new File(cachedAvatarPath))
                    .signature(new ObjectKey(timestamp))
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(holder.userImage);
        } else {
            // 设置默认头像
            Glide.with(context)
                    .load(R.drawable.ic_user)
                    .into(holder.userImage);

            // 在后台缓存头像
            String photoUrl = Constants.USER_PHOTO_BASE_URL + friendPhoto;
            cacheManager.cacheAvatar(friendUsername, photoUrl, new AvatarCacheManager.CacheCallback() {
                @Override
                public void onSuccess(String filePath) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // 使用缓存时间戳作为 signature
                        long timestamp = cacheManager.getAvatarTimestamp(friendUsername);
                        Glide.with(context)
                                .load(new File(filePath))
                                .signature(new ObjectKey(timestamp))
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(holder.userImage);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("ConversationAdapter", "Failed to cache avatar for " + friendUsername, e);
                }
            });
        }

        // 设置点击监听器
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, conversation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return timestamp;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView nicknameText;
        TextView latestMessageText;
        TextView timestampText;
        TextView unreadCountText;

        public ViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            nicknameText = itemView.findViewById(R.id.nickname_text);
            latestMessageText = itemView.findViewById(R.id.latest_message_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            unreadCountText = itemView.findViewById(R.id.unread_count_text);
        }
    }
}