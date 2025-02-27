package com.example.chat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "friend_remarks")
@Data
public class FriendRemark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @Column(length = 100)
    private String remark;
}