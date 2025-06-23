package com.example.chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class PostDTO {
    private Long id;
    private UserDTO user;
    private String title;
    private String content;
    private List<String> images;
    private String timestamp;
    private int likeCount;
    private boolean isLikedByCurrentUser;

}
