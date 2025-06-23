package com.example.chat.model;

import java.time.LocalDateTime;

public class PostLike {
    private Long id;
    private Post post;
    private User user;
    private LocalDateTime timestamp;

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public Post getPost(){
        return post;
    }

    public void setPost(Post post){
        this.post = post;
    }

    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public LocalDateTime getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp){
        this.timestamp = timestamp;
    }
}
