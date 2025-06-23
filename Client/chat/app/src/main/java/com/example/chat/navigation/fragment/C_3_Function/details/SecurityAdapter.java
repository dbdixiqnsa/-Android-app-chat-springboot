package com.example.chat.navigation.fragment.C_3_Function.details;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;

import java.util.ArrayList;
import java.util.List;

public class SecurityAdapter extends RecyclerView.Adapter<SecurityAdapter.SecurityViewHolder> {

    private List<SecurityItem> securityItems;
    private OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onDeleteClicked(SecurityItem item);
    }

    public SecurityAdapter(OnItemInteractionListener listener) {
        this.listener = listener;
        this.securityItems = new ArrayList<>();
    }

    public void updateData(List<SecurityItem> items) {
        this.securityItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SecurityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_security, parent, false);
        return new SecurityViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SecurityViewHolder holder, int position) {
        SecurityItem item = securityItems.get(position);
        holder.questionText.setText(item.getQuestion());
        holder.answerText.setText(item.getAnswer()); // 显示实际答案

        holder.deleteButton.setOnClickListener(v -> {
            listener.onDeleteClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return securityItems.size();
    }

    static class SecurityViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        TextView answerText;
        ImageButton deleteButton;

        public SecurityViewHolder(@NonNull View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.security_question);
            answerText = itemView.findViewById(R.id.security_answer);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
