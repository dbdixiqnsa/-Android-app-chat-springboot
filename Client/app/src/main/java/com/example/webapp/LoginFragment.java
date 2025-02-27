package com.example.webapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.webapp.activity.MainActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginFragment extends BottomSheetDialogFragment {

    private boolean isPasswordVisible = false; // 默认密码不可见
    private ExecutorService executorService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog); // 应用自定义样式
        executorService = Executors.newSingleThreadExecutor(); // 初始化线程池
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false); // 加载登录面板布局

        // 获取控件
        EditText usernameEditText = view.findViewById(R.id.username);
        EditText passwordEditText = view.findViewById(R.id.password);
        TextView showPasswordText = view.findViewById(R.id.show_password_text);
        Button loginButton = view.findViewById(R.id.btn_login_submit);

        // 切换密码显示状态
        showPasswordText.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                showPasswordText.setText("隐藏密码");
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                showPasswordText.setText("显示密码");
            }
            passwordEditText.setSelection(passwordEditText.getText().length()); // 确保光标位置在末尾
        });

        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (username.isEmpty()) {
                usernameEditText.setError("请输入账号");
                return;
            }
            if (password.isEmpty()) {
                passwordEditText.setError("请输入密码");
                return;
            }

            loginUser(username, password);
        });

        return view;
    }

    private void loginUser(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        FormBody requestBody = new FormBody.Builder()
                .add("userName", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url("http://192.168.123.70:8080/api/users/login")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() -> {
                    Log.e("LoginFragment", "网络请求失败", e);
                    Toast.makeText(getContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("LoginFragment", "Response code: " + response.code());
                Log.d("LoginFragment", "Response body: " + responseBody);

                getActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        if (response.code() == 401) {
                            // 特殊处理 401 错误
                            Toast.makeText(getContext(), "密码错误", Toast.LENGTH_SHORT).show();
                        } else if (responseBody.contains("登录成功")) {
                            Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                            getActivity().finish(); // 结束当前活动，防止用户按返回键回到登录页面
                            dismiss(); // 关闭 BottomSheet
                        } else {
                            handleLoginError(responseBody);
                        }
                    } else {
                        handleLoginError(responseBody);
                    }
                });
            }
        });
    }


    private void handleLoginError(String responseBody) {
        String errorMessage = responseBody.contains("无此用户") ? "无此用户，请前往注册" :
                responseBody.contains("该用户不是业务员") ? "该用户不是业务员，无法登录" :
                        "登录失败";
        Log.e("LoginFragment", "登录失败: " + errorMessage);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getParentFragmentManager().setFragmentResult("FragmentClosedKey", new Bundle());
        executorService.shutdown(); // 关闭线程池
    }
}
