package com.example.chat.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private Long id;
    private UserDTO user; // 对方用户的信息
    private String status;
    private String type;
}
