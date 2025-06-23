package com.example.chat.auth;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.MainActivity;
import com.example.chat.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 禁用默认的窗口动画
        getWindow().setWindowAnimations(0);
        setContentView(R.layout.activity_auth);
        // 初始化 WebView
        WebView webView = findViewById(R.id.webview_about);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        // 禁用滚动条
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        // 禁用回弹效果
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        // 设置 WebView 背景内容
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/html/background.html");
        // 按钮点击事件
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnSignUp = findViewById(R.id.btn_sign_up);

        btnLogin.setOnClickListener(v -> {
            BottomSheetDialogFragment loginSheet = new LoginRegisterBottomSheet(LoginRegisterBottomSheet.TYPE_LOGIN);
            loginSheet.show(getSupportFragmentManager(), loginSheet.getTag());
        });

        btnSignUp.setOnClickListener(v -> {
            BottomSheetDialogFragment registerSheet = new LoginRegisterBottomSheet(LoginRegisterBottomSheet.TYPE_REGISTER);
            registerSheet.show(getSupportFragmentManager(), registerSheet.getTag());
        });
    }

    public void onLoginSuccess() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
