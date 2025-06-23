package com.example.chat.navigation.fragment.C_2_Function;

public class MenuItem {
    private int iconResId;
    private String text;
    private int badgeCount;

    public MenuItem(int iconResId, String text, int badgeCount) {
        this.iconResId = iconResId;
        this.text = text;
        this.badgeCount = badgeCount;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getText() {
        return text;
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(int badgeCount) {
        this.badgeCount = badgeCount;
    }
}
