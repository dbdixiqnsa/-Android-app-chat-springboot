package com.example.chat.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.chat.R;

public class LoadingDialog extends Dialog {
    private TextView loadingText;
    private LottieAnimationView loadingAnimation;
    private long showTime;
    private static final long MIN_SHOW_TIME = 500; // 最小显示时间
    private boolean isDismissing = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LoadingDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_loading);

        loadingText = findViewById(R.id.loading_text);
        loadingAnimation = findViewById(R.id.loading_animation);

        // 设置动画背景透明和缩放模式
        loadingAnimation.setBackgroundColor(Color.TRANSPARENT);
        loadingAnimation.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void show() {
        showTime = System.currentTimeMillis();
        super.show();
    }

    @Override
    public void dismiss() {
        if (isDismissing) return;

        long diff = System.currentTimeMillis() - showTime;
        if (diff >= MIN_SHOW_TIME || !isShowing()) {
            super.dismiss();
        } else {
            isDismissing = true;
            handler.postDelayed(() -> {
                isDismissing = false;
                if (isShowing()) {
                    super.dismiss();
                }
            }, MIN_SHOW_TIME - diff);
        }
    }

    public void setLoadingText(String text) {
        if (loadingText != null) {
            loadingText.setText(text);
        }
    }
}