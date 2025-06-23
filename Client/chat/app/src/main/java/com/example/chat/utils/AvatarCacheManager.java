package com.example.chat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AvatarCacheManager {
    private Context context;
    private static final String PREFS_NAME = "avatar_prefs";
    private static final String TIMESTAMP_SUFFIX = "_timestamp";
    private final OkHttpClient client;

    public interface CacheCallback {
        void onSuccess(String filePath);
        void onFailure(Exception e);
    }

    public AvatarCacheManager(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String getCachedAvatarPath(String username) {
        File avatarFile = new File(context.getFilesDir(), username + "_avatar.jpg");
        return avatarFile.exists() ? avatarFile.getAbsolutePath() : null;
    }

    public long getAvatarTimestamp(String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(username + TIMESTAMP_SUFFIX, 0);
    }

    private void updateAvatarTimestamp(String username) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(username + TIMESTAMP_SUFFIX, System.currentTimeMillis());
        editor.apply();
    }

    public void cacheAvatar(String username, String url, CacheCallback callback) {
        File avatarFile = new File(context.getFilesDir(), username + "_avatar.jpg");

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure(new IOException("Unexpected code " + response));
                    return;
                }

                try (InputStream inputStream = response.body().byteStream();
                     FileOutputStream outputStream = new FileOutputStream(avatarFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    updateAvatarTimestamp(username);
                    callback.onSuccess(avatarFile.getAbsolutePath());
                } catch (IOException e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void clearCache() {
        File cacheDir = context.getFilesDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith("_avatar.jpg"));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        // 清除时间戳记录
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }
}