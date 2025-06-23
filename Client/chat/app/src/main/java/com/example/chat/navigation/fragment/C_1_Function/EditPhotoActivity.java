package com.example.chat.navigation.fragment.C_1_Function;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.chat.R;
import com.example.chat.utils.AvatarCacheManager;
import com.example.chat.utils.Constants;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditPhotoActivity extends AppCompatActivity {

    private ImageView currentPhoto;
    private Uri newPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photo);

        currentPhoto = findViewById(R.id.current_photo);
        Button selectPhotoButton = findViewById(R.id.select_photo_button);
        Button savePhotoButton = findViewById(R.id.save_photo_button);
        LinearLayout arrowLeftContainer = findViewById(R.id.arrow_left_container);

        arrowLeftContainer.setOnClickListener(v -> finish());

        // 加载当前头像（优先加载本地头像）
        String userPhotoPath = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userPhotoPath", null);
        if (userPhotoPath != null) {
            // 如果本地头像存在，则使用 Glide 加载
            File localPhotoFile = new File(userPhotoPath);
            if (localPhotoFile.exists()) {
                Glide.with(this)
                        .load(localPhotoFile)
                        .placeholder(R.drawable.ic_user)
                        .signature(new ObjectKey(System.currentTimeMillis())) // 强制 Glide 重新加载图片
                        .into(currentPhoto);
            } else {
                loadPhotoFromServer(); // 如果本地文件不存在，则尝试从服务器加载
            }
        } else {
            loadPhotoFromServer(); // 从服务器加载头像
        }
        // 选择新头像
        selectPhotoButton.setOnClickListener(v -> selectPhoto());
        // 保存头像
        savePhotoButton.setOnClickListener(v -> savePhoto());
    }

    private void loadPhotoFromServer() {
        String userPhoto = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("userPhoto", "default.jpg");
        Glide.with(this)
                .load(Constants.USER_PHOTO_BASE_URL + userPhoto)
                .placeholder(R.drawable.ic_user)
                .into(currentPhoto);
    }

    private void selectPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            photoPickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开图片选择器", Toast.LENGTH_SHORT).show();
        }
    }

    private final ActivityResultLauncher<Intent> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            startCrop(selectedImageUri);
                        } catch (Exception e) {
                            Toast.makeText(this, "处理图片时出错: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private void startCrop(Uri sourceUri) {
        String destinationFileName = "CROP_" + UUID.randomUUID().toString() + ".jpg";
        Uri destinationUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(getCacheDir(), destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setFreeStyleCropEnabled(false); // 禁用自由裁剪
        options.setCircleDimmedLayer(false); // 禁用圆形裁剪

        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // 设置为正方形
                .withMaxResultSize(500, 500)
                .withOptions(options)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                newPhotoUri = resultUri;
                currentPhoto.setImageURI(newPhotoUri);  // 更新 UI 中的头像
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(this, "裁剪失败: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePhoto() {
        if (newPhotoUri == null) {
            Toast.makeText(this, "未选择新头像", Toast.LENGTH_SHORT).show();
            return;
        }

        // 上传新头像到服务器
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", null);
        if (username != null) {
            uploadPhoto(newPhotoUri, username);
        } else {
            Toast.makeText(this, "当前用户未登录", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPhoto(Uri photoUri, String username) {
        OkHttpClient client = new OkHttpClient();
        MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

        // 获取真实文件路径
        String realPath = getRealPathFromURI(this, photoUri);
        if (realPath == null) {
            Toast.makeText(this, "无法获取图片路径", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建文件
        File photoFile = new File(realPath);
        if (!photoFile.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示上传进度对话框
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("上传头像...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 创建 Multipart 请求体
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", username)
                .addFormDataPart("photo", photoFile.getName(),
                        RequestBody.create(photoFile, MEDIA_TYPE_JPEG))
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url(Constants.UPDATE_PHOTO_URL)
                .post(requestBody)
                .build();

        // 异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditPhotoActivity.this, "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 在主线程处理响应
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        String newPhotoName = "";
                        try {
                            newPhotoName = response.body() != null ? response.body().string() : "";
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!newPhotoName.isEmpty()) {
                            // 将新头像保存到 External Files 目录
                            File newPhoto = new File(getExternalFilesDir(null), "user_photo.jpg");
                            try (FileInputStream fis = new FileInputStream(photoFile);
                                 FileOutputStream fos = new FileOutputStream(newPhoto)) {
                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, length);
                                }
                            } catch (Exception e) {
                                Toast.makeText(EditPhotoActivity.this, "保存新头像失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 更新 SharedPreferences 中的头像信息
                            getSharedPreferences("user_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("userPhoto", newPhotoName)  // 保存新的头像文件名
                                    .putString("userPhotoPath", newPhoto.getAbsolutePath())  // 保存本地路径
                                    .apply();

                            // 更新头像缓存
                            AvatarCacheManager cacheManager = new AvatarCacheManager(EditPhotoActivity.this);
                            cacheManager.cacheAvatar(username, Constants.USER_PHOTO_BASE_URL + newPhotoName,
                                    new AvatarCacheManager.CacheCallback() {
                                        @Override
                                        public void onSuccess(String path) {
                                            Log.d("EditPhotoActivity", "Avatar cache updated successfully");
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e("EditPhotoActivity", "Failed to update avatar cache", e);
                                        }
                                    });

                            // 使用 Glide 刷新 UI 中的头像
                            Glide.with(EditPhotoActivity.this)
                                    .load(newPhoto)
                                    .signature(new ObjectKey(System.currentTimeMillis()))
                                    .into(currentPhoto);

                            Toast.makeText(EditPhotoActivity.this, "头像修改成功", Toast.LENGTH_SHORT).show();

                            // 发送本地广播通知其他组件刷新
                            Intent intent = new Intent("ACTION_USER_PHOTO_UPDATED");
                            LocalBroadcastManager.getInstance(EditPhotoActivity.this).sendBroadcast(intent);

                            finish(); // 返回上一页
                        } else {
                            Toast.makeText(EditPhotoActivity.this, "上传失败: 服务器未返回新头像名称", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(EditPhotoActivity.this, "上传失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private String getRealPathFromURI(Context context, Uri uri) {
        String realPath = null;
        
        // 使用 DocumentFile 方式获取
        try {
            if (uri.getScheme().equals("content")) {
                String[] projection = { MediaStore.Images.Media.DATA };
                try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        realPath = cursor.getString(columnIndex);
                    }
                }
            }
            // 如果是 file:// 类型的 URI
            else if (uri.getScheme().equals("file")) {
                realPath = uri.getPath();
            }
        } catch (Exception e) {
            Log.e("EditPhotoActivity", "Error getting real path", e);
        }

        // 复制文件到应用私有目录
        if (realPath == null) {
            try {
                File destinationFile = new File(context.getCacheDir(), "temp_" + System.currentTimeMillis() + ".jpg");
                try (InputStream is = context.getContentResolver().openInputStream(uri);
                     OutputStream os = new FileOutputStream(destinationFile)) {
                    if (is != null) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        realPath = destinationFile.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                Log.e("EditPhotoActivity", "Error copying file", e);
            }
        }

        Log.d("EditPhotoActivity", "Real path: " + realPath);
        return realPath;
    }
}
