package com.example.chat.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "post_likes", uniqueConstraints = {@UniqueConstraint(columnNames = {"post_id", "user_id"})})
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 点赞的动态
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 点赞的用户
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 使用 @CreationTimestamp 自动设置创建时间
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
