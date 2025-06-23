package com.example.chat.navigation.fragment.C_2_Function;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;

import java.util.List;

public class CustomMenuAdapter extends RecyclerView.Adapter<CustomMenuAdapter.ViewHolder> {

    public interface OnMenuItemClickListener {
        void onMenuItemClick(int position);
    }

    private Context context;
    private List<MenuItem> menuItems;
    private OnMenuItemClickListener listener;

    public CustomMenuAdapter(Context context, List<MenuItem> menuItems, OnMenuItemClickListener listener) {
        this.context = context;
        this.menuItems = menuItems;
        this.listener = listener;
    }

    @Override
    public CustomMenuAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.menu_item_with_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CustomMenuAdapter.ViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.text.setText(item.getText());

        if (item.getBadgeCount() > 0) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(String.valueOf(item.getBadgeCount()));
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuItemClick(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView text;
        TextView badge;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.menu_item_icon);
            text = itemView.findViewById(R.id.menu_item_text);
            badge = itemView.findViewById(R.id.menu_item_badge);
        }
    }
}
