package com.example.webapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.FormBody;

import org.json.JSONObject;

import java.io.IOException;

public class SignUpFragment extends BottomSheetDialogFragment {

    private boolean isPasswordVisible = false; // 默认密码不可见

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog); // 应用自定义样式
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false); // 加载注册面板布局

        // 获取控件
        EditText userNameEditText = view.findViewById(R.id.signup_username);
        EditText passwordEditText = view.findViewById(R.id.signup_password);
        EditText confirmPasswordEditText = view.findViewById(R.id.signup_confirm_password);
        TextView showPasswordText = view.findViewById(R.id.show_password_text);
        Button signUpButton = view.findViewById(R.id.btn_sign_up_submit);

        // 设置点击事件
        showPasswordText.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible; // 切换密码显示状态

            // 切换密码输入类型
            if (isPasswordVisible) {
                passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                showPasswordText.setText("隐藏密码");
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                showPasswordText.setText("显示密码");
            }

            // 重新设置光标位置到文本末尾
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // 注册按钮点击事件
        signUpButton.setOnClickListener(v -> {
            String userName = userNameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // 验证输入
            if (userName.isEmpty()) {
                userNameEditText.setError("请输入账号");
            } else if (password.isEmpty()) {
                passwordEditText.setError("请输入密码");
            } else if (confirmPassword.isEmpty()) {
                confirmPasswordEditText.setError("请确认密码");
            } else if (!password.equals(confirmPassword)) {
                // 显示密码不匹配的提示
                confirmPasswordEditText.setError("密码不匹配");
            } else {
                // 所有验证通过，调用注册方法
                registerUser(userName, password);
            }
        });

        return view;
    }

    private void registerUser(String userName, String password) {
        OkHttpClient client = new OkHttpClient();

        // 创建请求体
        RequestBody requestBody = new FormBody.Builder()
                .add("userName", userName)
                .add("password", password)
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url("http://192.168.123.70:8080/api/users/register") // 本地服务器地址
                .post(requestBody)
                .build();

        // 异步请求
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                // 在UI线程中更新UI，例如显示错误信息
                getActivity().runOnUiThread(() -> {
                    Log.e("SignUpFragment", "网络请求失败", e);
                    Toast.makeText(getContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("SignUpFragment", "Response code: " + response.code());
                Log.d("SignUpFragment", "Response body: " + responseBody);

                getActivity().runOnUiThread(() -> {
                    try {
                        if (response.isSuccessful()) {
                            // 注册成功
                            Log.i("SignUpFragment", "注册成功");
                            Toast.makeText(getContext(), "注册成功", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            // 解析错误信息并显示
                            JSONObject jsonObject = new JSONObject(responseBody);
                            String errorMessage = jsonObject.optString("error", "注册失败");
                            Log.e("SignUpFragment", "注册失败: " + errorMessage);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("SignUpFragment", "解析响应失败", e);
                        Toast.makeText(getContext(), "注册失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 在面板关闭时，向 L_R_Activity 发送结果
        getParentFragmentManager().setFragmentResult("FragmentClosedKey", new Bundle());
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(android.app.Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT); // 确保背景透明
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
            params.height = CoordinatorLayout.LayoutParams.WRAP_CONTENT; // 使底部面板高度适应内容
            bottomSheet.setLayoutParams(params);
        }
    }
}