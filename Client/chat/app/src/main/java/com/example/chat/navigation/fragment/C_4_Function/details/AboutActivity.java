package com.example.chat.navigation.fragment.C_4_Function.details;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.example.chat.R;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        WebView webView = findViewById(R.id.webview_about);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        arrowLeftContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setVerticalScrollBarEnabled(false); // 禁用垂直滚动条
        webView.setHorizontalScrollBarEnabled(false); // 禁用水平滚动条
        // 禁用回弹效果
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        // 加载位于 assets/html/index.html 的 HTML 文件
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/html/index.html");
    }
}
