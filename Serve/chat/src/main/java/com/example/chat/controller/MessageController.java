package com.example.chat.controller;

import com.example.chat.dto.MessageDTO;
import com.example.chat.model.Message;
import com.example.chat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 发送消息
    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody MessageDTO messageDTO) {
        try {
            Message message = messageService.sendMessage(
                    messageDTO.getSenderUsername(),
                    messageDTO.getReceiverUsername(),
                    messageDTO.getContent()
            );
            MessageDTO responseDTO = new MessageDTO(message);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 获取用户的最近联系人及其最新消息
    @PostMapping("/latest")
    public ResponseEntity<List<MessageDTO>> getLatestMessages(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        try {
            List<Message> messages = messageService.getLatestMessages(username);
            List<MessageDTO> dtoList = messages.stream().map(MessageDTO::new).collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 获取两个用户之间的消息
    @PostMapping("/chat")
    public ResponseEntity<List<MessageDTO>> getMessages(@RequestBody Map<String, String> payload) {
        String senderUsername = payload.get("senderUsername");
        String receiverUsername = payload.get("receiverUsername");
        try {
            List<Message> messages = messageService.getMessages(senderUsername, receiverUsername);
            List<MessageDTO> dtoList = messages.stream().map(MessageDTO::new).collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 标记消息为已读
    @PostMapping("/markAsRead")
    public ResponseEntity<Void> markMessagesAsRead(@RequestBody Map<String, String> payload) {
        String senderUsername = payload.get("senderUsername");
        String receiverUsername = payload.get("receiverUsername");
        try {
            messageService.markMessagesAsRead(senderUsername, receiverUsername);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 获取未读消息数量
    @GetMapping("/unreadCounts")
    public ResponseEntity<List<Map<String, Object>>> getUnreadCounts(@RequestParam String username) {
        try {
            List<Object[]> counts = messageService.getUnreadMessageCounts(username);
            List<Map<String, Object>> response = counts.stream().map(arr -> {
                Map<String, Object> map = new HashMap<>();
                map.put("senderUsername", arr[0]);
                map.put("unreadCount", arr[1]);
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
