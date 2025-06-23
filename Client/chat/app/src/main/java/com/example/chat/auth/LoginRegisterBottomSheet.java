package com.example.chat.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.chat.R;
import com.example.chat.dto.PostDTO;
import com.example.chat.model.User;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;
import com.example.chat.utils.LoadingDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LoginRegisterBottomSheet extends BottomSheetDialogFragment {

    public static final int TYPE_LOGIN = 0;
    public static final int TYPE_REGISTER = 1;

    private int type;
    private boolean isPasswordVisible = false;
    private boolean isSignupPasswordVisible = false;

    private static final String EMAIL_PATTERN = 
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public LoginRegisterBottomSheet(int type) {
        this.type = type;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        if (type == TYPE_LOGIN) {
            view = inflater.inflate(R.layout.fragment_login, container, false);

            EditText username = view.findViewById(R.id.username);
            EditText password = view.findViewById(R.id.password);
            Button btnLogin = view.findViewById(R.id.btn_login_submit);

            btnLogin.setOnClickListener(v -> {
                String user = username.getText().toString();
                String pass = password.getText().toString();
                if (user.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(getContext(), "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                login(user, pass);
            });

        } else {
            view = inflater.inflate(R.layout.fragment_sign_up, container, false);

            EditText username = view.findViewById(R.id.signup_username);
            EditText email = view.findViewById(R.id.signup_email);
            EditText password = view.findViewById(R.id.signup_password);
            EditText confirmPassword = view.findViewById(R.id.signup_confirm_password);
            Button btnRegister = view.findViewById(R.id.btn_sign_up_submit);

            btnRegister.setOnClickListener(v -> {
                String user = username.getText().toString().trim();
                String emailText = email.getText().toString().trim();
                String pass = password.getText().toString();
                String confirmPass = confirmPassword.getText().toString();

                // 获取 TextInputLayout
                TextInputLayout emailLayout = view.findViewById(R.id.email_input_layout);

                // 验证各个字段
                if (user.isEmpty()) {
                    Toast.makeText(getContext(), "用户名不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 邮箱验证
                if (emailText.isEmpty()) {
                    emailLayout.setError("邮箱不能为空");
                    return;
                }
                if (!emailText.matches(EMAIL_PATTERN)) {
                    emailLayout.setError("请输入有效的邮箱地址");
                    return;
                } else {
                    emailLayout.setError(null); // 清除错误提示
                }

                if (pass.isEmpty()) {
                    Toast.makeText(getContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (confirmPass.isEmpty()) {
                    Toast.makeText(getContext(), "确认密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!pass.equals(confirmPass)) {
                    Toast.makeText(getContext(), "两次密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                register(user, emailText, pass);
            });

            EditText emailInput = view.findViewById(R.id.signup_email);
            TextInputLayout emailLayout = view.findViewById(R.id.email_input_layout);

            emailInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String email = s.toString().trim();
                    String error = validateEmail(email);
                    if (error != null) {
                        emailLayout.setError(error);
                    } else {
                        emailLayout.setError(null);
                        emailLayout.setHelperText(getString(R.string.email_format_valid));
                    }
                }
            });
        }
        return view;
    }

    private void login(String username, String password) {
        // 显示加载对话框
        LoadingDialog loadingDialog = new LoadingDialog(getContext());
        loadingDialog.show();

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String loginJson = "{" +
                "\"username\":\"" + username + "\"," +
                "\"password\":\"" + password + "\"" +
                "}";

        RequestBody requestBody = RequestBody.create(loginJson, JSON);
        Request request = new Request.Builder()
                .url(Constants.LOGIN_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingDialog.dismiss(); // 隐藏加载对话框
                    Toast.makeText(getContext(), "网络请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingDialog.dismiss(); // 隐藏加载对话框
                    if (response.isSuccessful()) {
                        // 登录成功，保存用户信息
                        saveUserDetails(username);
                    } else {
                        // 处理不同的错误状态码
                        switch (response.code()) {
                            case 404:
                                // 用户不存在
                                Toast.makeText(getContext(), "无此用户，请注册", Toast.LENGTH_SHORT).show();
                                break;
                            case 401:
                                // 密码错误
                                Toast.makeText(getContext(), "账号或密码错误", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                // 其他错误
                                Toast.makeText(getContext(), "登录失败: " + responseBody, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
            }
        });
    }


    private void saveUserDetails(String username) {
        // 显示加载对话框
        LoadingDialog loadingDialog = new LoadingDialog(getContext());
        loadingDialog.show();

        OkHttpClient client = new OkHttpClient();
        String url = Constants.USER_DETAILS_URL;

        String userDetailsJson = "{" +
                "\"username\":\"" + username + "\"" +
                "}";

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(userDetailsJson, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(getContext(), "获取用户信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("UserDetails", "Response: " + responseBody); // 添加日志

                        JSONObject json = new JSONObject(responseBody);
                        String nickname = json.optString("nickname", "默认名字");
                        String userPhoto = json.optString("userPhoto", "default.jpg");
                        String email = json.optString("email", "");

                        // 保存用户信息到 SharedPreferences
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences("user_prefs", getContext().MODE_PRIVATE).edit();
                        editor.putString("username", username);
                        editor.putString("nickname", nickname);
                        editor.putString("email", email);
                        editor.putString("userPhoto", userPhoto);
                        editor.apply();

                        // 先下载当前用户头像
                        downloadUserPhoto(userPhoto, new DownloadCallback() {
                            @Override
                            public void onSuccess(String path) {
                                // 当前用户头像下载成功后，继续更新好友头像
                                refreshFriendAvatars(username, loadingDialog);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(getContext(), "下载头像失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    // 即使当前用户头像下载失败，也继续更新好友头像
                                    refreshFriendAvatars(username, loadingDialog);
                                });
                            }
                        });

                    } catch (Exception e) {
                        Log.e("UserDetails", "Parse error: ", e); // 添加详细错误日志
                        new Handler(Looper.getMainLooper()).post(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(getContext(), "解析用户信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(getContext(), "获取用户信息失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // 下载回调接口
    interface DownloadCallback {
        void onSuccess(String path);
        void onFailure(Exception e);
    }

    private void refreshFriendAvatars(String username, LoadingDialog loadingDialog) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        // 获取好友列表和动态列表
        retrofit2.Call<List<User>> friendCall = apiService.getFriendsList(username);
        retrofit2.Call<List<PostDTO>> timelineCall = apiService.getTimelinePosts(username);

        friendCall.enqueue(new retrofit2.Callback<List<User>>() {
            @Override
            public void onResponse(retrofit2.Call<List<User>> call, retrofit2.Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> friends = response.body();

                    // 获取时间线上的帖子
                    timelineCall.enqueue(new retrofit2.Callback<List<PostDTO>>() {
                        @Override
                        public void onResponse(retrofit2.Call<List<PostDTO>> call, retrofit2.Response<List<PostDTO>> timelineResponse) {
                            if (timelineResponse.isSuccessful() && timelineResponse.body() != null) {
                                List<PostDTO> posts = timelineResponse.body();
                                // 合并好友列表和动态作者列表，确保所有用户的头像都会被缓存
                                Set<String> allUsernames = new HashSet<>();
                                for (User friend : friends) {
                                    allUsernames.add(friend.getUsername());
                                }
                                for (PostDTO post : posts) {
                                    allUsernames.add(post.getUser().getUsername());
                                }

                                // 如果没有需要更新的头像，直接完成登录流程
                                if (allUsernames.isEmpty()) {
                                    completeLoginProcess(loadingDialog, false);
                                    return;
                                }

                                // 清除所有旧的头像缓存
                                AvatarCacheManager cacheManager = new AvatarCacheManager(getContext());
                                cacheManager.clearCache();

                                final int[] downloadCount = {allUsernames.size()};
                                final boolean[] hasError = {false};

                                // 下载所有用户的头像
                                for (String userUsername : allUsernames) {
                                    String photoUrl = Constants.USER_PHOTO_BASE_URL;
                                    // 从好友列表或帖子列表中找到对应的用户照片
                                    String userPhoto = findUserPhoto(userUsername, friends, posts);
                                    if (userPhoto != null) {
                                        photoUrl += userPhoto;
                                    } else {
                                        photoUrl += "default.jpg";
                                    }

                                    Log.d("Avatar", "Downloading avatar for " + userUsername + " from " + photoUrl);

                                    // 下载并缓存新的头像
                                    cacheManager.cacheAvatar(userUsername, photoUrl, new AvatarCacheManager.CacheCallback() {
                                        @Override
                                        public void onSuccess(String filePath) {
                                            Log.d("Avatar", "Successfully updated avatar for " + userUsername);
                                            checkDownloadComplete();
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e("Avatar", "Failed to update avatar for " + userUsername, e);
                                            hasError[0] = true;
                                            checkDownloadComplete();
                                        }

                                        private void checkDownloadComplete() {
                                            synchronized (downloadCount) {
                                                downloadCount[0]--;
                                                if (downloadCount[0] == 0) {
                                                    completeLoginProcess(loadingDialog, hasError[0]);
                                                }
                                            }
                                        }
                                    });
                                }
                            } else {
                                // 时间线获取失败，仍然继续处理好友头像
                                processFriendsAvatars(friends, loadingDialog);
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<List<PostDTO>> call, Throwable t) {
                            Log.e("Timeline", "Failed to get timeline", t);
                            // 时间线获取失败，仍然继续处理好友头像
                            processFriendsAvatars(friends, loadingDialog);
                        }
                    });
                } else {
                    completeLoginProcess(loadingDialog, true);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<User>> call, Throwable t) {
                Log.e("Avatar", "Failed to get friends list", t);
                completeLoginProcess(loadingDialog, true);
            }
        });
    }

    private void processFriendsAvatars(List<User> friends, LoadingDialog loadingDialog) {
        if (friends.isEmpty()) {
            completeLoginProcess(loadingDialog, false);
            return;
        }

        AvatarCacheManager cacheManager = new AvatarCacheManager(getContext());
        cacheManager.clearCache();

        final int[] downloadCount = {friends.size()};
        final boolean[] hasError = {false};

        for (User friend : friends) {
            String photoUrl = Constants.USER_PHOTO_BASE_URL + friend.getUserPhoto();
            cacheManager.cacheAvatar(friend.getUsername(), photoUrl, new AvatarCacheManager.CacheCallback() {
                @Override
                public void onSuccess(String filePath) {
                    checkDownloadComplete();
                }

                @Override
                public void onFailure(Exception e) {
                    hasError[0] = true;
                    checkDownloadComplete();
                }

                private void checkDownloadComplete() {
                    synchronized (downloadCount) {
                        downloadCount[0]--;
                        if (downloadCount[0] == 0) {
                            completeLoginProcess(loadingDialog, hasError[0]);
                        }
                    }
                }
            });
        }
    }

    private void completeLoginProcess(LoadingDialog loadingDialog, boolean hasError) {
        new Handler(Looper.getMainLooper()).post(() -> {
            loadingDialog.dismiss();

            // 发送广播通知所有界面刷新头像
            Intent intent = new Intent("ACTION_USER_PHOTO_UPDATED");
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);

            // 显示适当的提示信息
            if (hasError) {
                Toast.makeText(getContext(), "登录成功，但部分头像更新失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
            }

            // 更新登录状态和UI
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).onLoginSuccess();
            }
        });
    }

    private String findUserPhoto(String username, List<User> friends, List<PostDTO> posts) {
        // 先在好友列表中查找
        for (User friend : friends) {
            if (friend.getUsername().equals(username)) {
                return friend.getUserPhoto();
            }
        }
        // 再在帖子作者中查找
        for (PostDTO post : posts) {
            if (post.getUser().getUsername().equals(username)) {
                return post.getUser().getUserPhoto();
            }
        }
        return null;
    }


    private void downloadUserPhoto(String photoName, final DownloadCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = Constants.USER_PHOTO_BASE_URL + photoName;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    File photoFile = new File(getActivity().getExternalFilesDir(null), "user_photo.jpg");
                    try (ResponseBody body = response.body()) {
                        if (body != null) {
                            byte[] bytes = body.bytes();
                            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                                fos.write(bytes);
                            }

                            // 保存头像路径
                            SharedPreferences.Editor editor = getActivity()
                                    .getSharedPreferences("user_prefs", getContext().MODE_PRIVATE)
                                    .edit();
                            editor.putString("userPhotoPath", photoFile.getAbsolutePath());
                            editor.apply();

                            callback.onSuccess(photoFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new IOException("Download failed: " + response.message()));
                }
            }
        });
    }

    private void register(String username, String email, String password) {
        Log.d("Register", "Starting registration process");
        Log.d("Register", "Password null check: " + (password == null));
        Log.d("Register", "Password empty check: " + (password.isEmpty()));
        Log.d("Register", "Password length: " + password.length());

        if (username == null || username.isEmpty() || 
            email == null || email.isEmpty() || 
            password == null || password.isEmpty()) {
            Log.e("Register", "Invalid parameters detected");
            Toast.makeText(getContext(), "注册参数无效", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", username);
            jsonObject.put("email", email);
            jsonObject.put("password", password);
            jsonObject.put("nickname", username);
            jsonObject.put("userPhoto", "default.jpg");

            String registerJson = jsonObject.toString();
            Log.d("Register", "Full request body: " + registerJson);

            RequestBody requestBody = RequestBody.create(JSON, registerJson);
            Request request = new Request.Builder()
                    .url(Constants.REGISTER_URL)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            Log.d("Register", "Request URL: " + request.url());
            Log.d("Register", "Request headers: " + request.headers());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("Register", "Registration failed", e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "注册失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("Register", "Response code: " + response.code());
                    Log.d("Register", "Response headers: " + response.headers());
                    Log.d("Register", "Response body: " + responseBody);
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (response.isSuccessful()) {
                            if (responseBody.contains("注册成功")) {
                                Toast.makeText(getContext(), "注册成功", Toast.LENGTH_SHORT).show();
                                dismiss();
                            } else {
                                Toast.makeText(getContext(), responseBody, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "注册失败: " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e("Register", "Error creating request", e);
            Toast.makeText(getContext(), "注册失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String validateEmail(String email) {
        if (email.isEmpty()) {
            return getString(R.string.error_email_empty);
        }
        if (!email.contains("@")) {
            return getString(R.string.error_email_at_symbol);
        }
        if (!email.contains(".")) {
            return getString(R.string.error_email_dot);
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return getString(R.string.error_email_format);
        }
        if (parts[0].isEmpty()) {
            return getString(R.string.error_email_local_part);
        }
        if (parts[1].isEmpty() || !parts[1].contains(".")) {
            return getString(R.string.error_email_domain);
        }
        if (!email.matches(EMAIL_PATTERN)) {
            return getString(R.string.error_email_invalid_chars);
        }
        return null; // 验证通过
    }
}
