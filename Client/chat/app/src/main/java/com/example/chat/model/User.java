package com.example.chat.model;

public class User {
    private Long id;
    private String username;
    private String nickname;
    private String userPhoto;
    private String photoUpdateTime;

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getNickname(){
        return nickname;
    }

    public void setNickname(String nickname){
        this.nickname = nickname;
    }

    public String getUserPhoto(){
        return userPhoto;
    }

    public void setUserPhoto(String userPhoto){
        this.userPhoto = userPhoto;
    }


    public String getPhotoUpdateTime() { return photoUpdateTime; }

    public void setPhotoUpdateTime(String photoUpdateTime) { this.photoUpdateTime = photoUpdateTime; }
}
