package com.example.chat.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "security_questions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question"}))
public class SecurityQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false)
    private String answer;
}
