package com.example.chat.navigation.fragment.B_1_Function;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.dto.PostDTO;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPostDialog extends DialogFragment {

    public interface AddPostListener {
        void onPostAdded();
    }

    private EditText editTitle;
    private EditText editContent;
    private Button buttonSelectImages;
    private Button buttonSubmit;
    private RecyclerView imagesRecyclerView;
    private SelectedImagesAdapter selectedImagesAdapter;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private String currentUsername;
    private AddPostListener listener;
    private ImageButton buttonBack;
    private TextView toolbarTitle;

    private static final int MAX_IMAGE_COUNT = 4;

    // 使用 ActivityResultLauncher 处理图片选择
    private final ActivityResultLauncher<Intent> pickImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                for (int i = 0; i < count; i++) {
                                    if (selectedImagesAdapter.getItemCount() >= MAX_IMAGE_COUNT) {
                                        Toast.makeText(getContext(), "最多可上传4张图片", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                    selectedImagesAdapter.addImage(imageUri);
                                }
                            } else if (data.getData() != null) {
                                if (selectedImagesAdapter.getItemCount() >= MAX_IMAGE_COUNT) {
                                    Toast.makeText(getContext(), "最多可上传4张图片", Toast.LENGTH_SHORT).show();
                                } else {
                                    Uri imageUri = data.getData();
                                    selectedImagesAdapter.addImage(imageUri);
                                }
                            }
                        }
                    });

    public AddPostDialog(String currentUsername, AddPostListener listener) {
        this.currentUsername = currentUsername;
        this.listener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置对话框的宽度为匹配父布局
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // 使用自定义布局
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_post, null);
        dialog.setContentView(view);

        // 禁止点击对话框外部关闭
        dialog.setCanceledOnTouchOutside(false);

        // 初始化视图组件
        buttonBack = view.findViewById(R.id.button_back);
        toolbarTitle = view.findViewById(R.id.text_toolbar_title);
        editTitle = view.findViewById(R.id.edit_post_title);
        editContent = view.findViewById(R.id.edit_post_content);
        buttonSelectImages = view.findViewById(R.id.button_select_images);
        buttonSubmit = view.findViewById(R.id.button_submit_post);
        imagesRecyclerView = view.findViewById(R.id.images_recycler_view);

        // 设置返回按钮点击事件
        buttonBack.setOnClickListener(v -> dismiss());

        // 初始化 RecyclerView 使用 GridLayoutManager
        selectedImagesAdapter = new SelectedImagesAdapter(getContext(), selectedImageUris, new SelectedImagesAdapter.OnImageListChangeListener() {
            @Override
            public void onImageListEmpty() {
                // 处理图片列表为空的情况，例如隐藏 RecyclerView
                Toast.makeText(getContext(), "没有选择任何图片", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onImageListMaxReached() {
                // 显示最多可上传4张图片的提示
                Toast.makeText(getContext(), "最多可上传4张图片", Toast.LENGTH_SHORT).show();
            }
        });

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2); // 每行2列
        imagesRecyclerView.setLayoutManager(gridLayoutManager);
        imagesRecyclerView.setAdapter(selectedImagesAdapter);

        // 设置选择图片按钮点击事件
        buttonSelectImages.setOnClickListener(v -> {
            // 检查存储权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 请求权限
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
                } else {
                    // 权限已授予，打开图片选择器
                    openImagePicker();
                }
            } else { // Android 12 及以下
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 请求权限
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                } else {
                    // 权限已授予，打开图片选择器
                    openImagePicker();
                }
            }
        });

        // 设置提交按钮点击事件
        buttonSubmit.setOnClickListener(v -> submitPost());

        return dialog;
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(getContext(), "权限被拒绝，无法选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 打开图片选择器
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        pickImagesLauncher.launch(Intent.createChooser(intent, "选择图片"));
    }

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private static final int PROGRESS_DELAY = 200; // 200ms更新一次

    // 提交帖子
    private void submitPost() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editTitle.setError("标题不能为空");
            editTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            editContent.setError("内容不能为空");
            editContent.requestFocus();
            return;
        }

        // 创建并显示进度对话框
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("发布动态");
        progressDialog.setMessage("正在处理图片...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 准备上传的数据
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), currentUsername);
        RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content);

        // 开始进度动画，从0开始
        final int[] currentProgress = {0};
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentProgress[0] < 90) {
                    currentProgress[0]++;
                    String message = currentProgress[0] < 30 ? "准备图片..." :
                            currentProgress[0] < 60 ? "正在处理图片..." :
                                    "正在上传...";
                    updateProgress(progressDialog, message, currentProgress[0]);
                    progressHandler.postDelayed(this, PROGRESS_DELAY);
                }
            }
        };
        progressHandler.post(progressRunnable);

        // 在后台线程处理图片
        new Thread(() -> {
            List<MultipartBody.Part> imageParts = new ArrayList<>();
            int totalImages = selectedImagesAdapter.getImageUris().size();

            for (Uri uri : selectedImagesAdapter.getImageUris()) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                    bitmap = getResizedBitmap(bitmap, 1024, 1024);

                    String filename = UUID.randomUUID().toString() + ".jpg";
                    File file = new File(requireContext().getCacheDir(), filename);
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    fos.flush();
                    fos.close();

                    RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/*"));
                    MultipartBody.Part body = MultipartBody.Part.createFormData("images", file.getName(), requestFile);
                    imageParts.add(body);
                } catch (IOException e) {
                    e.printStackTrace();
                    progressHandler.removeCallbacks(progressRunnable);
                    updateProgress(progressDialog, "图片处理失败", -1);
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "图片处理失败", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 在主线程中执行网络请求
            requireActivity().runOnUiThread(() -> {
                Call<PostDTO> call = apiService.createPost(usernameBody, titleBody, contentBody, imageParts);
                call.enqueue(new Callback<PostDTO>() {
                    @Override
                    public void onResponse(Call<PostDTO> call, Response<PostDTO> response) {
                        // 停止进度动画
                        progressHandler.removeCallbacks(progressRunnable);

                        if (response.isSuccessful() && response.body() != null) {
                            // 快速完成到100%
                            updateProgress(progressDialog, "发布成功", 100);
                            new Handler().postDelayed(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "发布成功", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onPostAdded();
                                }
                                dismiss();
                            }, 500); // 延迟500ms关闭，让用户看到100%
                        } else {
                            progressDialog.dismiss();
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "未知错误";
                                Toast.makeText(getContext(), "发布失败: " + errorBody, Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "发布失败: " + response.message(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostDTO> call, Throwable t) {
                        // 停止进度动画
                        progressHandler.removeCallbacks(progressRunnable);
                        progressDialog.dismiss();

                        if (t instanceof java.net.SocketTimeoutException) {
                            Toast.makeText(getContext(),
                                    "网络请求超时，请稍后在动态列表查看是否发布成功",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    "网络请求失败: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理Handler的回调，防止内存泄漏
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }


    // 更新进度对话框的工具方法
    private void updateProgress(ProgressDialog progressDialog, String message, int progress) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (progressDialog.isShowing()) {
                    progressDialog.setMessage(message);
                    if (progress >= 0) {
                        progressDialog.setProgress(progress);
                    }
                }
            });
        }
    }

    // 调整图片大小的方法
    private Bitmap getResizedBitmap(Bitmap bm, int maxWidth, int maxHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);

        if (scale >= 1) {
            return bm; // 无需缩放
        }

        int newWidth = Math.round(scale * width);
        int newHeight = Math.round(scale * height);

        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, false);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

    }
}
