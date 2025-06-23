package com.example.chat.navigation.fragment.C_2_Function;

import com.example.chat.model.User;

public class FriendListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private int type;
    private String header;
    private User user;

    public FriendListItem(int type, String header, User user) {
        this.type = type;
        this.header = header;
        this.user = user;
    }

    public int getType() {
        return type;
    }

    public String getHeader() {
        return header;
    }

    public User getUser() {
        return user;
    }
}
