package com.example.chat.dto;

import com.example.chat.model.Message;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long id;
    private String senderUsername;
    private String receiverUsername;
    private String content;
    private LocalDateTime timestamp;

    public MessageDTO() {
    }

    public MessageDTO(Message message) {
        this.id = message.getId();
        this.senderUsername = message.getSender().getUsername();
        this.receiverUsername = message.getReceiver().getUsername();
        this.content = message.getContent();
        this.timestamp = message.getTimestamp();
    }
}
