package com.example.chat.navigation.fragment.B_1_Function;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chat.R;

import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder> {

    private static final int MAX_IMAGES = 4; // 定义最大图片数量

    private List<Uri> imageUris;
    private OnImageListChangeListener changeListener;
    private Context context;

    /**
     * 更新接口，添加当图片数量达到上限时的回调方法
     */
    public interface OnImageListChangeListener {
        void onImageListEmpty();
        void onImageListMaxReached();
    }

    /**
     * 修改构造函数，接受Context和新的监听器接口
     */
    public SelectedImagesAdapter(Context context, List<Uri> imageUris, OnImageListChangeListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.changeListener = listener;
    }

    @NonNull
    @Override
    public SelectedImagesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_image, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedImagesAdapter.ViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        Glide.with(holder.imageView.getContext())
                .load(uri)
                .into(holder.imageView);

        holder.imageRemove.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && currentPosition < imageUris.size()) {
                imageUris.remove(currentPosition);
                notifyItemRemoved(currentPosition);
                notifyItemRangeChanged(currentPosition, imageUris.size());
                if (imageUris.isEmpty() && changeListener != null) {
                    changeListener.onImageListEmpty();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    /**
     * ViewHolder 保持对视图的引用
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView imageRemove;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_selected);
            imageRemove = itemView.findViewById(R.id.image_remove);
        }
    }

    /**
     * 添加图片的方法，检查是否超过最大数量
     */
    public void addImage(Uri uri) {
        if (imageUris.size() >= MAX_IMAGES) {
            if (changeListener != null) {
                changeListener.onImageListMaxReached();
            }
            return;
        }
        imageUris.add(uri);
        notifyItemInserted(imageUris.size() - 1);
    }

    /**
     * 允许外部获取当前图片列表
     */
    public List<Uri> getImageUris() {
        return imageUris;
    }
}
