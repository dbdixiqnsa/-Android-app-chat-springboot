package com.example.chat.navigation.fragment.B_1_Function;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Outline;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.R;
import com.example.chat.dto.PostDTO;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostDTO> postList;
    private Context context;
    private String currentUsername;
    private String userPhotoPath;
    private ApiService apiService;
    private OnPostLikeListener likeListener;
    private AvatarCacheManager cacheManager;

    public interface OnPostLikeListener {
        void onPostLiked(PostDTO post);
    }

    public PostAdapter(List<PostDTO> postList, Context context, String currentUsername, String userPhotoPath, OnPostLikeListener likeListener) {
        this.postList = postList;
        this.context = context;
        this.currentUsername = currentUsername;
        this.userPhotoPath = userPhotoPath;
        this.likeListener = likeListener;
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.cacheManager = new AvatarCacheManager(context);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostDTO post = postList.get(position);
        // 判断帖子是否属于当前用户
        boolean isCurrentUser = post.getUser().getUsername().equals(currentUsername);

        if (isCurrentUser) {
            // 对于当前用户，优先使用缓存的头像
            String cachedAvatarPath = cacheManager.getCachedAvatarPath(currentUsername);
            if (cachedAvatarPath != null && new File(cachedAvatarPath).exists()) {
                // 使用缓存时间戳作为 signature
                long timestamp = cacheManager.getAvatarTimestamp(currentUsername);
                Glide.with(context)
                        .load(new File(cachedAvatarPath))
                        .signature(new ObjectKey(timestamp))
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(holder.imageUserPhoto);
            } else if (userPhotoPath != null && new File(userPhotoPath).exists()) {
                // 如果缓存不存在，尝试使用本地头像
                long timestamp = new File(userPhotoPath).lastModified();
                Glide.with(context)
                        .load(new File(userPhotoPath))
                        .signature(new ObjectKey(timestamp))
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(holder.imageUserPhoto);
            } else {
                // 如果本地头像也不存在，加载服务器头像
                loadServerPhoto(holder, post);
            }
        } else {
            // 对于其他用户，使用正常的加载逻辑
            loadServerPhoto(holder, post);
        }
        // 设置用户名
        String displayName = post.getUser().getNickname() != null && !post.getUser().getNickname().isEmpty()
                ? post.getUser().getNickname()
                : post.getUser().getUsername();
        holder.textUsername.setText(displayName);
        // 设置时间戳
        String formattedTimestamp = formatTimestamp(post.getTimestamp());
        holder.textTimestamp.setText(formattedTimestamp);
        // 设置帖子标题和内容
        holder.textPostTitle.setText(post.getTitle());
        holder.textPostContent.setText(post.getContent());
        // 设置动态图片
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            holder.imagesRecyclerView.setVisibility(View.VISIBLE);
            holder.imageAdapter.updateImages(post.getImages());
        } else {
            holder.imagesRecyclerView.setVisibility(View.GONE);
        }
        // 设置点赞数
        holder.textLikeCount.setText(String.valueOf(post.getLikeCount()));
        // 设置点赞按钮状态
        if (post.isLikedByCurrentUser()) { // 使用 isLikedByCurrentUser()
            holder.buttonLike.setImageResource(R.drawable.ic_like_filled); // 点赞选中图标
        } else {
            holder.buttonLike.setImageResource(R.drawable.ic_like_outline); // 点赞未选中图标
        }
        // 设置删除按钮的可见性
        if (currentUsername.equals(post.getUser().getUsername())) {
            holder.buttonDeletePost.setVisibility(View.VISIBLE);
        } else {
            holder.buttonDeletePost.setVisibility(View.GONE);
        }
        // 点赞按钮点击事件
        holder.buttonLike.setOnClickListener(v -> {
            toggleLike(post, holder);
        });
        // 删除按钮点击事件
        holder.buttonDeletePost.setOnClickListener(v -> {
            // 显示确认删除的对话框
            showDeleteConfirmationDialog(post, holder.getAdapterPosition());
        });
    }
    @Override
    public int getItemCount() {
        return postList.size();
    }
    /**
     * 加载服务器头像的方法
     * @param holder 当前ViewHolder
     * @param post   当前帖子
     */
    private void loadServerPhoto(PostViewHolder holder, PostDTO post) {
        String username = post.getUser().getUsername();
        String cachedAvatarPath = cacheManager.getCachedAvatarPath(username);

        if (cachedAvatarPath != null && new File(cachedAvatarPath).exists()) {
            // 使用缓存的头像和时间戳
            long timestamp = cacheManager.getAvatarTimestamp(username);
            Glide.with(context)
                    .load(new File(cachedAvatarPath))
                    .signature(new ObjectKey(timestamp))
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(holder.imageUserPhoto);
        } else {
            // 如果缓存中没有，显示默认头像并尝试下载
            Glide.with(context)
                    .load(R.drawable.ic_user)
                    .into(holder.imageUserPhoto);

            // 在后台缓存头像
            if (post.getUser().getUserPhoto() != null && !post.getUser().getUserPhoto().isEmpty()) {
                String photoUrl = Constants.USER_PHOTO_BASE_URL + post.getUser().getUserPhoto();
                cacheManager.cacheAvatar(username, photoUrl, new AvatarCacheManager.CacheCallback() {
                    @Override
                    public void onSuccess(String filePath) {
                        // 在主线程中更新 UI
                        new Handler(Looper.getMainLooper()).post(() -> {
                            long timestamp = cacheManager.getAvatarTimestamp(username);
                            Glide.with(context)
                                    .load(new File(filePath))
                                    .signature(new ObjectKey(timestamp))
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_user)
                                    .into(holder.imageUserPhoto);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("PostAdapter", "Failed to cache avatar for " + username, e);
                    }
                });
            }
        }
    }
    /**
     * 切换点赞状态
     * @param post   当前帖子
     * @param holder 当前ViewHolder
     */
    private void toggleLike(PostDTO post, PostViewHolder holder) {
        Call<Map<String, String>> call = apiService.toggleLikePost(post.getId(), currentUsername);
        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    if (message != null) {
                        if (message.equals("点赞成功")) {
                            post.setLikedByCurrentUser(true);
                            post.setLikeCount(post.getLikeCount() + 1);
                            holder.buttonLike.setImageResource(R.drawable.ic_like_filled);
                            holder.textLikeCount.setText(String.valueOf(post.getLikeCount()));
                        } else if (message.equals("取消点赞")) {
                            post.setLikedByCurrentUser(false);
                            post.setLikeCount(post.getLikeCount() - 1);
                            holder.buttonLike.setImageResource(R.drawable.ic_like_outline);
                            holder.textLikeCount.setText(String.valueOf(post.getLikeCount()));
                        }
                        if (likeListener != null) {
                            likeListener.onPostLiked(post);
                        }
                    } else {
                        Toast.makeText(context, "操作失败: 无返回消息", Toast.LENGTH_SHORT).show();
                        Log.e("PostAdapter", "操作失败: 无返回消息");
                    }
                } else {
                    Toast.makeText(context, "操作失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("PostAdapter", "操作失败: " + response.code() + " " + response.message());
                }
            }
            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(context, "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PostAdapter", "网络请求失败: ", t);
            }
        });
    }

    /**
     * 显示删除确认对话框
     * @param post      要删除的帖子
     * @param position  帖子在列表中的位置
     */
    private void showDeleteConfirmationDialog(PostDTO post, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("确认删除")
                .setMessage("您确定要删除这条动态吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deletePost(post, position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除动态
     * @param post     要删除的帖子
     * @param position 帖子在列表中的位置
     */
    private void deletePost(PostDTO post, int position) {
        Call<Map<String, String>> call = apiService.deletePost(post.getId(), currentUsername);
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("删除中...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        call.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    if (message != null && message.equals("动态删除成功")) {
                        Toast.makeText(context, "动态已删除", Toast.LENGTH_SHORT).show();
                        // 从列表中移除动态并通知适配器
                        postList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, postList.size());
                        if (likeListener != null) {
                            likeListener.onPostLiked(null);
                        }
                    } else {
                        Toast.makeText(context, "删除失败: " + message, Toast.LENGTH_SHORT).show();
                        Log.e("PostAdapter", "删除失败: " + message);
                    }
                } else {
                    Toast.makeText(context, "删除失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("PostAdapter", "删除失败: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(context, "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PostAdapter", "网络请求失败: ", t);
            }
        });
    }

    /**
     * 格式化时间戳字符串
     * @param timestamp 原始时间戳字符串
     * @return 格式化后的时间字符串
     */
    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "未知时间";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            if (date == null) {
                return "未知时间";
            }
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return timestamp; // 解析失败时返回原始字符串
        }
    }

    /**
     * ViewHolder 类，用于缓存帖子视图组件
     */
    public class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageUserPhoto;
        TextView textUsername;
        TextView textTimestamp;
        TextView textPostTitle;
        TextView textPostContent;
        RecyclerView imagesRecyclerView;
        ImageButton buttonLike;
        ImageButton buttonDeletePost;
        TextView textLikeCount;
        ImageAdapter imageAdapter;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化视图组件
            imageUserPhoto = itemView.findViewById(R.id.image_user_photo);
            textUsername = itemView.findViewById(R.id.text_username);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textPostTitle = itemView.findViewById(R.id.text_post_title);
            textPostContent = itemView.findViewById(R.id.text_post_content);
            imagesRecyclerView = itemView.findViewById(R.id.images_recycler_view);
            buttonLike = itemView.findViewById(R.id.button_like);
            buttonDeletePost = itemView.findViewById(R.id.button_delete_post);
            textLikeCount = itemView.findViewById(R.id.text_like_count);

            // 初始化 ImageAdapter
            imageAdapter = new ImageAdapter(context);

            // 创建一个动态的 GridLayoutManager
            GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 2);
            // 设置 Span 大小的回调
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    // 如果只有一张图片，让它占据整行
                    return imageAdapter.getItemCount() == 1 ? 2 : 1;
                }
            });
            imagesRecyclerView.setLayoutManager(gridLayoutManager);
            imagesRecyclerView.setAdapter(imageAdapter);

            // 设置间距
            int spacingInPixels = itemView.getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            imagesRecyclerView.addItemDecoration(
                    new GridSpacingItemDecoration(2, spacingInPixels, true, 0)
            );

            // 移除固定高度设置
            imagesRecyclerView.setHasFixedSize(false);
        }

        // 绑定数据到视图的方法
        public void bind(PostDTO post) {
            textUsername.setText(post.getUser().getNickname());
            textTimestamp.setText(formatTimestamp(post.getTimestamp()));
            textPostTitle.setText(post.getTitle());
            textPostContent.setText(post.getContent());
            textLikeCount.setText(String.valueOf(post.getLikeCount()));

            // 更新图片列表
            if (post.getImages() != null && !post.getImages().isEmpty()) {
                imagesRecyclerView.setVisibility(View.VISIBLE);
                imageAdapter.updateImages(post.getImages());

                // 根据图片数量调整布局参数
                ViewGroup.LayoutParams params = imagesRecyclerView.getLayoutParams();
                if (post.getImages().size() == 1) {
                    // 单张图片时使用固定宽高比
                    params.height = context.getResources()
                            .getDimensionPixelSize(R.dimen.single_image_height);
                } else {
                    // 多张图片时使用 wrap_content
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                imagesRecyclerView.setLayoutParams(params);
            } else {
                imagesRecyclerView.setVisibility(View.GONE);
            }

            // 设置点赞状态
            buttonLike.setImageResource(
                    post.isLikedByCurrentUser() ?
                            R.drawable.ic_like_filled :
                            R.drawable.ic_like_outline
            );

            // 设置删除按钮可见性
            buttonDeletePost.setVisibility(
                    currentUsername.equals(post.getUser().getUsername()) ?
                            View.VISIBLE :
                            View.GONE
            );
        }
    }

    /**
     * 内部适配器，用于显示帖子中的图片
     */
    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private List<String> imageList;
        private Context context;

        public ImageAdapter(Context context) { // 修改构造函数
            this.context = context;
        }

        /**
         * 更新图片列表并刷新
         *
         * @param images 新的图片列表
         */
        public void updateImages(List<String> images) {
            this.imageList = images;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ImageAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageAdapter.ImageViewHolder holder, int position) {
            if (imageList == null || position >= imageList.size()) {
                return;
            }
            String imageUrl = imageList.get(position);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder) // 加载中的占位符
                    .error(R.drawable.ic_image_error) // 加载失败时显示的图标
                    .into(holder.imagePost);
        }

        @Override
        public int getItemCount() {
            return imageList != null ? imageList.size() : 0;
        }

        /**
         * ImageViewHolder 类，用于缓存帖子图片视图组件
         */
        public class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imagePost;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imagePost = itemView.findViewById(R.id.image_post);
            }
        }
    }

    /**
     * 更新用户头像路径并刷新适配器
     *
     * @param newPhotoPath 新的头像文件路径
     */
    public void updateUserPhotoPath(String newPhotoPath) {
        this.userPhotoPath = newPhotoPath;
        notifyDataSetChanged(); // 刷新所有视图
    }
}
