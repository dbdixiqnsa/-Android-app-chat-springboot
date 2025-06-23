package com.example.chat.service;

import com.example.chat.model.Message;
import com.example.chat.model.User;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    // 发送消息
    public Message sendMessage(String senderUsername, String receiverUsername, String content) {
        User sender = userRepository.findByUsername(senderUsername);
        User receiver = userRepository.findByUsername(receiverUsername);
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        return messageRepository.save(message);
    }

    // 获取两个用户之间的所有消息（双向）
    public List<Message> getMessages(String user1Username, String user2Username) {
        User user1 = userRepository.findByUsername(user1Username);
        User user2 = userRepository.findByUsername(user2Username);
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return messageRepository.findMessagesBetweenUsers(user1, user2);
    }

    // 获取用户的最近联系人及其最新消息
    public List<Message> getLatestMessages(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return messageRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
    }

    // 标记消息为已读
    public void markMessagesAsRead(String senderUsername, String receiverUsername) {
        User sender = userRepository.findByUsername(senderUsername);
        User receiver = userRepository.findByUsername(receiverUsername);
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        List<Message> messages = messageRepository.findUnreadMessagesBetweenUsers(sender, receiver);
        for (Message message : messages) {
            message.setRead(true); // 注意这里使用 setRead
        }
        messageRepository.saveAll(messages);
    }

    // 获取未读消息数量
    public List<Object[]> getUnreadMessageCounts(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return messageRepository.countUnreadMessagesGroupBySender(user);
    }

    // 获取两个用户之间的最新一条消息
    public Message getLatestMessage(String user1Username, String user2Username) {
        User user1 = userRepository.findByUsername(user1Username);
        User user2 = userRepository.findByUsername(user2Username);
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Pageable pageable = PageRequest.of(0, 1);
        List<Message> messages = messageRepository.findLatestMessageBetweenUsers(user1, user2, pageable);
        if (messages != null && !messages.isEmpty()) {
            return messages.get(0);
        } else {
            return null;
        }
    }
}
