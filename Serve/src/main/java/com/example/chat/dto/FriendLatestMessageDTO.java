package com.example.chat.dto;

import lombok.Data;

@Data
public class FriendLatestMessageDTO {
    private UserDTO friend;
    private MessageDTO latestMessage;
}
