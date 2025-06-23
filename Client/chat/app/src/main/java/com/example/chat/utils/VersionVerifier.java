package com.example.chat.utils;

import android.content.Context;
import android.util.Log;
import com.example.chat.network.VersionApiClient;
import com.example.chat.network.VersionApiService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VersionVerifier {
    private Context context;
    private VersionVerificationCallback callback;
    private static final String TAG = "VersionVerifier";

    public interface VersionVerificationCallback {
        void onVersionVerified();
        void onVersionMismatch(String serverVersion);
        void onError(String error);
    }

    public VersionVerifier(Context context, VersionVerificationCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void verifyVersion() {
        VersionApiService apiService = VersionApiClient.getClient().create(VersionApiService.class);

        Map<String, String> payload = new HashMap<>();
        payload.put("version", Constants.CLIENT_VERSION);

        Log.d(TAG, "开始版本验证，客户端版本: " + Constants.CLIENT_VERSION);

        Call<Map<String, Object>> call = apiService.verifyVersion(payload);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    boolean success = (boolean) result.get("success");
                    Log.d(TAG, "版本验证响应: " + result);

                    if (success) {
                        callback.onVersionVerified();
                    } else {
                        String serverVersion = (String) result.get("serverVersion");
                        callback.onVersionMismatch(serverVersion);
                    }
                } else {
                    String errorMsg = "请求失败: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "读取错误响应失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
}