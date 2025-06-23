package com.example.chat.navigation.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.chat.R;
import com.example.chat.dto.PostDTO;
import com.example.chat.navigation.fragment.B_1_Function.AddPostDialog;
import com.example.chat.navigation.fragment.B_1_Function.PostAdapter;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BFragment extends Fragment {

    private RecyclerView postsRecyclerView;
    private FloatingActionButton fabAddPost;
    private PostAdapter postAdapter;
    private List<PostDTO> postList = new ArrayList<>();
    private String currentUsername;
    private String userPhotoPath;

    // 定义广播接收器
    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_REFRESH_TIMELINE".equals(intent.getAction())) {
                fetchTimelinePosts();
            }
        }
    };

    public BFragment() {
    }

    public static BFragment newInstance() {
        return new BFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("ACTION_REFRESH_TIMELINE");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 取消注册广播接收器
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshReceiver);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_b, container, false);

        postsRecyclerView = view.findViewById(R.id.posts_recycler_view);
        fabAddPost = view.findViewById(R.id.fab_add_post);

        // 获取当前用户名和头像路径
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Activity.MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("username", null);
        userPhotoPath = sharedPreferences.getString("userPhotoPath", null); // 获取头像路径

        if (currentUsername == null) {
            Toast.makeText(getContext(), "用户未登录", Toast.LENGTH_SHORT).show();
            return view;
        }

        postAdapter = new PostAdapter(postList, getContext(), currentUsername, userPhotoPath, this::onPostLiked);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postsRecyclerView.setAdapter(postAdapter);

        fabAddPost.setOnClickListener(v -> showAddPostDialog());

        // 初始获取时间线动态
        fetchTimelinePosts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新动态列表
        fetchTimelinePosts();
    }

    /**
     * 获取时间线动态（包括自己的和好友的）
     */
    private void fetchTimelinePosts() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<PostDTO>> call = apiService.getTimelinePosts(currentUsername);
        call.enqueue(new Callback<List<PostDTO>>() {
            @Override
            public void onResponse(Call<List<PostDTO>> call, Response<List<PostDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body());
                    // 按时间降序排列
                    postList.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));
                    postAdapter.notifyDataSetChanged();
                } else {
                    if (response.code() == 404) { // 使用数字代码404
                        Toast.makeText(getContext(), "没有动态", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "获取动态失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<PostDTO>> call, Throwable t) {
                Toast.makeText(getContext(), "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 显示发布动态的对话框
    private void showAddPostDialog() {
        AddPostDialog dialog = new AddPostDialog(currentUsername, new AddPostDialog.AddPostListener() {
            @Override
            public void onPostAdded() {
                // 发布成功后刷新动态列表
                fetchTimelinePosts();
            }
        });
        dialog.setCancelable(true);
        dialog.show(getChildFragmentManager(), "AddPostDialog");
    }

    // 点赞状态变化的回调
    private void onPostLiked(PostDTO post) {
    }
}
