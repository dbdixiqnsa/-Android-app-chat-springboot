package com.example.chat.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class MessageDTO {

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

    @SerializedName("isRead")
    private boolean isRead; // 新增字

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    private boolean isFailed = false;

    public boolean isFailed() {
        return isFailed;
    }

    public void setFailed(boolean failed) {
        isFailed = failed;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageDTO that = (MessageDTO) o;

        return isRead == that.isRead &&
                Objects.equals(id, that.id) &&
                Objects.equals(senderUsername, that.senderUsername) &&
                Objects.equals(receiverUsername, that.receiverUsername) &&
                Objects.equals(content, that.content) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, senderUsername, receiverUsername, content, timestamp, isRead);
    }
}
