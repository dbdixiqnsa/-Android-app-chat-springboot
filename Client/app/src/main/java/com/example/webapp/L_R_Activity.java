package com.example.webapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;


public class L_R_Activity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_l_r); // 使用布局文件

        btnLogin = findViewById(R.id.btn_login);
        btnSignUp = findViewById(R.id.btn_sign_up);

        // 点击 Log In 按钮，显示 LoginFragment
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginFragment();
            }
        });

        // 点击 Sign Up 按钮，显示 SignUpFragment
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignUpFragment();
            }
        });
    }

    // 显示登录面板
    private void showLoginFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LoginFragment loginFragment = new LoginFragment();
        loginFragment.show(fragmentManager, "login_fragment");

        // 隐藏按钮
        toggleButtonsVisibility(View.GONE);

        // 监听 LoginFragment 关闭事件
        fragmentManager.setFragmentResultListener("FragmentClosedKey", this, (requestKey, result) -> {
            toggleButtonsVisibility(View.VISIBLE); // 显示按钮
        });
    }

    // 显示注册面板
    private void showSignUpFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SignUpFragment signUpFragment = new SignUpFragment();
        signUpFragment.show(fragmentManager, "signup_fragment");

        // 隐藏按钮
        toggleButtonsVisibility(View.GONE);

        // 监听 SignUpFragment 关闭事件
        fragmentManager.setFragmentResultListener("FragmentClosedKey", this, (requestKey, result) -> {
            toggleButtonsVisibility(View.VISIBLE); // 显示按钮
        });
    }

    // 切换按钮的可见性
    private void toggleButtonsVisibility(int visibility) {
        btnLogin.setVisibility(visibility);
        btnSignUp.setVisibility(visibility);
    }
}
