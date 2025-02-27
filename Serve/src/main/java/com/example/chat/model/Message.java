package com.example.chat.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 发送者
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 接收者
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // 消息内容
    @Column(nullable = false)
    private String content;

    // 时间戳
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // 已读状态
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // Getter 和 Setter 方法
    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
}
