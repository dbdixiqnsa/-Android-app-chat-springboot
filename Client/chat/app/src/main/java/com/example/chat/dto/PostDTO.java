package com.example.chat.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PostDTO {
    private Long id;
    private UserDTO user;
    private String title;
    private String content;
    private List<String> images;
    private String timestamp;
    private int likeCount;

    @SerializedName("likedByCurrentUser")
    private boolean likedByCurrentUser;

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public UserDTO getUser(){
        return user;
    }

    public void setUser(UserDTO user){
        this.user = user;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getContent(){
        return content;
    }

    public void setContent(String content){
        this.content = content;
    }

    public List<String> getImages(){
        return images;
    }

    public void setImages(List<String> images){
        this.images = images;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public int getLikeCount(){
        return likeCount;
    }

    public void setLikeCount(int likeCount){
        this.likeCount = likeCount;
    }

    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }
}
