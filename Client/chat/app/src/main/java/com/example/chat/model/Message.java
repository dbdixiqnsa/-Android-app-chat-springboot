package com.example.chat.model;

import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("id")
    private Long id;

    @SerializedName("senderUsername")
    private String senderUsername;

    @SerializedName("receiverUsername")
    private String receiverUsername;

    @SerializedName("content")
    private String content;

    @SerializedName("timestamp")
    private String timestamp;

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
