package com.example.chat.navigation.fragment.C_2_Function;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.R;
import com.example.chat.model.User;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;

import java.io.File;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<FriendListItem> friendListItems;
    private OnFriendLongClickListener onFriendLongClickListener;
    private Context context;
    private String userPhotoPath;
    private AvatarCacheManager cacheManager;

    public FriendsAdapter(List<FriendListItem> friendListItems, Context context, String userPhotoPath) {
        this.friendListItems = friendListItems;
        this.context = context;
        this.userPhotoPath = userPhotoPath;
        this.cacheManager = new AvatarCacheManager(context);
    }

    public interface OnFriendLongClickListener {
        void onLongClick(User user, int position);
    }

    public void setOnFriendLongClickListener(OnFriendLongClickListener listener) {
        this.onFriendLongClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return friendListItems.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return friendListItems.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == FriendListItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false);
            return new FriendsViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FriendListItem item = friendListItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).letterTextView.setText(item.getHeader());
        } else if (holder instanceof FriendsViewHolder) {
            FriendsViewHolder friendsViewHolder = (FriendsViewHolder) holder;
            User user = item.getUser();

            friendsViewHolder.nicknameTextView.setText(user.getNickname());
            friendsViewHolder.usernameTextView.setText(user.getUsername());

            // 判断是否是当前用户
            boolean isCurrentUser = user.getUsername().equals(getCurrentUsername());

            if (isCurrentUser && userPhotoPath != null) {
                loadLocalAvatar(friendsViewHolder.userImageView, user);
            } else {
                loadCachedAvatar(friendsViewHolder.userImageView, user);
            }

            // 设置长按事件
            friendsViewHolder.itemView.setOnLongClickListener(v -> {
                if (onFriendLongClickListener != null) {
                    onFriendLongClickListener.onLongClick(user, position);
                }
                return true;
            });
        }
    }

    private void loadLocalAvatar(ImageView imageView, User user) {
        File localPhotoFile = new File(userPhotoPath);
        if (localPhotoFile.exists()) {
            // 使用时间戳作为 signature
            long timestamp = cacheManager.getAvatarTimestamp(user.getUsername());
            Glide.with(context)
                    .load(localPhotoFile)
                    .signature(new ObjectKey(timestamp))
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imageView);
        } else {
            loadCachedAvatar(imageView, user);
        }
    }

    private void loadCachedAvatar(ImageView imageView, User user) {
        String cachedAvatarPath = cacheManager.getCachedAvatarPath(user.getUsername());

        if (cachedAvatarPath != null && new File(cachedAvatarPath).exists()) {
            // 使用时间戳作为 signature
            long timestamp = cacheManager.getAvatarTimestamp(user.getUsername());
            Glide.with(context)
                    .load(new File(cachedAvatarPath))
                    .signature(new ObjectKey(timestamp))
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(imageView);
        } else {
            // 如果缓存中没有，显示默认头像并尝试缓存
            Glide.with(context)
                    .load(R.drawable.ic_user)
                    .into(imageView);

            // 在后台缓存头像
            String photoUrl = Constants.USER_PHOTO_BASE_URL + user.getUserPhoto();
            cacheManager.cacheAvatar(user.getUsername(), photoUrl, new AvatarCacheManager.CacheCallback() {
                @Override
                public void onSuccess(String filePath) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // 使用时间戳作为 signature
                        long timestamp = cacheManager.getAvatarTimestamp(user.getUsername());
                        Glide.with(context)
                                .load(new File(filePath))
                                .signature(new ObjectKey(timestamp))
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(imageView);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("FriendsAdapter", "Failed to cache avatar for " + user.getUsername(), e);
                }
            });
        }
    }

    private String getCurrentUsername() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("username", null);
    }

    public void updateUserPhotoPath(String newPhotoPath) {
        this.userPhotoPath = newPhotoPath;
        notifyDataSetChanged();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView letterTextView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            letterTextView = itemView.findViewById(R.id.letter_text);
        }
    }

    static class FriendsViewHolder extends RecyclerView.ViewHolder {
        TextView nicknameTextView;
        TextView usernameTextView;
        ImageView userImageView;

        public FriendsViewHolder(View itemView) {
            super(itemView);
            nicknameTextView = itemView.findViewById(R.id.nickname_text);
            usernameTextView = itemView.findViewById(R.id.username_text);
            userImageView = itemView.findViewById(R.id.user_image);
        }
    }
}