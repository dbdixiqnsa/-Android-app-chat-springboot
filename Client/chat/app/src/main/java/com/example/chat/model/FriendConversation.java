package com.example.chat.model;

public class FriendConversation {
    private User friend;
    private int unreadCount;
    private String latestMessage;
    private String timestamp;

    public FriendConversation(User friend, int unreadCount) {
        this.friend = friend;
        this.unreadCount = unreadCount;
    }

    public User getFriend() {
        return friend;
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(String latestMessage) {
        this.latestMessage = latestMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
