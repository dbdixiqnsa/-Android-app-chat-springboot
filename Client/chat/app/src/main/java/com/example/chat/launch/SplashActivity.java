package com.example.chat.launch;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.example.chat.R;
import com.example.chat.auth.AuthActivity;
import com.example.chat.utils.VersionVerifier;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DISPLAY_LENGTH = 1200;
    private LottieAnimationView lottieAnimationView;
    private VersionVerifier versionVerifier;
    private final Handler handler = new Handler();  // 单一 Handler 实例

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initializeLottieAnimation();
        verifyAppVersion();
    }

    private void initializeLottieAnimation() {
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        lottieAnimationView.enableMergePathsForKitKatAndAbove(true);
    }

    private void verifyAppVersion() {
        versionVerifier = new VersionVerifier(this, new VersionVerifier.VersionVerificationCallback() {
            @Override
            public void onVersionVerified() {
                navigateToAuth();
            }

            @Override
            public void onVersionMismatch(String serverVersion) {
                showVersionMismatchError(serverVersion);
            }

            @Override
            public void onError(String error) {
                showError(error);
            }
        });
        versionVerifier.verifyVersion();
    }

    private void navigateToAuth() {
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }

    private void showVersionMismatchError(String serverVersion) {
        Toast.makeText(this,
                "当前版本过低，请获得最新版本安装包：" + serverVersion,
                Toast.LENGTH_LONG).show();
        handler.postDelayed(this::finish, 3500);
    }

    private void showError(String error) {
        Toast.makeText(this, "版本验证失败: " + error, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);  // 清除所有待处理的消息
        if (lottieAnimationView != null) {
            lottieAnimationView.cancelAnimation();
        }
    }
}