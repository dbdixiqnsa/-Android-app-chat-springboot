package com.example.chat.navigation.fragment.C_2_Function;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chat.R;
import com.example.chat.model.FriendRequest;
import com.example.chat.utils.Constants;
import java.util.List;

public class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestsAdapter.FriendRequestViewHolder> {

    private List<FriendRequest> friendRequests;
    private AcceptRequestListener acceptRequestListener;
    private DeclineRequestListener declineRequestListener;

    public FriendRequestsAdapter(List<FriendRequest> friendRequests,
                                 AcceptRequestListener acceptRequestListener,
                                 DeclineRequestListener declineRequestListener) {
        this.friendRequests = friendRequests;
        this.acceptRequestListener = acceptRequestListener;
        this.declineRequestListener = declineRequestListener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = friendRequests.get(position);

        // 限制名称最多显示6个字符，超出部分用“...”表示
        String nickname = request.getUser().getNickname();
        if (nickname.length() > 6) {
            nickname = nickname.substring(0, 6) + "...";
        }
        holder.nicknameTextView.setText(nickname);

        // 限制用户名最多显示6个字符，超出部分用“...”表示
        String username = request.getUser().getUsername();
        if (username.length() > 6) {
            username = username.substring(0, 6) + "...";
        }
        holder.usernameTextView.setText(username);

        // 加载头像
        String userPhotoName = request.getUser().getUserPhoto();
        if (userPhotoName == null || userPhotoName.isEmpty()) {
            userPhotoName = "default.jpg";
        }
        String userPhotoUrl = Constants.USER_PHOTO_BASE_URL + userPhotoName;

        // 添加唯一标识符（时间戳）以防止 Glide 使用缓存的旧图片
        String uniqueUrl = userPhotoUrl + "?t=" + System.currentTimeMillis();

        Glide.with(holder.itemView.getContext())
                .load(uniqueUrl)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(holder.userImageView);

        // 根据类型和状态设置UI
        if ("RECEIVED".equals(request.getType())) {
            // 收到的好友申请
            if ("PENDING".equals(request.getStatus())) {
                // 显示“同意”和“拒绝”按钮
                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.declineButton.setVisibility(View.VISIBLE);
                holder.statusTextView.setVisibility(View.GONE);
            } else {
                // 已处理的申请，显示状态
                holder.acceptButton.setVisibility(View.GONE);
                holder.declineButton.setVisibility(View.GONE);
                holder.statusTextView.setVisibility(View.VISIBLE);
                holder.statusTextView.setText(getStatusText(request.getStatus()));
            }
        } else {
            // 自己发送的好友申请
            holder.acceptButton.setVisibility(View.GONE);
            holder.declineButton.setVisibility(View.GONE);
            holder.statusTextView.setVisibility(View.VISIBLE);
            holder.statusTextView.setText(getStatusText(request.getStatus()));
        }

        // 设置按钮的点击事件
        holder.acceptButton.setOnClickListener(v -> acceptRequestListener.onAcceptRequest(request.getId()));
        holder.declineButton.setOnClickListener(v -> declineRequestListener.onDeclineRequest(request.getId()));
    }


    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "等待通过";
            case "ACCEPTED":
                return "已通过";
            case "DECLINED":
                return "已拒绝";
            default:
                return "";
        }
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    public interface AcceptRequestListener {
        void onAcceptRequest(Long requestId);
    }

    public interface DeclineRequestListener {
        void onDeclineRequest(Long requestId);
    }

    static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView nicknameTextView;
        TextView usernameTextView;
        ImageView userImageView;
        Button acceptButton;
        Button declineButton;
        TextView statusTextView;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            nicknameTextView = itemView.findViewById(R.id.nickname_text);
            usernameTextView = itemView.findViewById(R.id.username_text);
            userImageView = itemView.findViewById(R.id.user_image);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
            statusTextView = itemView.findViewById(R.id.status_text);
        }
    }
}
