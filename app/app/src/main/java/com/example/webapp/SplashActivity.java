package com.example.webapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 启动画面显示时长，2秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 延迟启动主活动
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 启动主活动并切换主题
                Intent intent = new Intent(SplashActivity.this, L_R_Activity.class);
                startActivity(intent);
                finish(); // 结束 SplashActivity
            }
        }, SPLASH_DISPLAY_LENGTH); // 延迟时间，单位为毫秒
    }
}
