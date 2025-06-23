package com.example.chat.navigation.fragment.A_1_Function;

import androidx.recyclerview.widget.DiffUtil;

import com.example.chat.dto.MessageDTO;

import java.util.List;

public class ChatDiffCallback extends DiffUtil.Callback {

    private final List<MessageDTO> oldList;
    private final List<MessageDTO> newList;

    private boolean hasNewMessages = false;

    public ChatDiffCallback(List<MessageDTO> oldList, List<MessageDTO> newList) {
        this.oldList = oldList;
        this.newList = newList;

        // 在构造函数中检测是否有新消息
        if (newList.size() > oldList.size()) {
            hasNewMessages = true;
        }
    }

    public boolean hasNewMessages() {
        return hasNewMessages;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        MessageDTO oldItem = oldList.get(oldItemPosition);
        MessageDTO newItem = newList.get(newItemPosition);
        if (oldItem.getId() != null && newItem.getId() != null) {
            return oldItem.getId().equals(newItem.getId());
        } else {
            return oldItem.getSenderUsername().equals(newItem.getSenderUsername())
                    && oldItem.getReceiverUsername().equals(newItem.getReceiverUsername())
                    && oldItem.getTimestamp().equals(newItem.getTimestamp());
        }
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        MessageDTO oldItem = oldList.get(oldItemPosition);
        MessageDTO newItem = newList.get(newItemPosition);
        return oldItem.equals(newItem);
    }
}
