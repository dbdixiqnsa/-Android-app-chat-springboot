package com.example.chat.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "friend_requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"from_user_id", "to_user_id"})
})
@Data
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @Column(name = "request_date", nullable = false, updatable = false)
    private LocalDateTime requestDate = LocalDateTime.now();

    @Column(name = "is_viewed", nullable = false)
    private Boolean isViewed = false;

    @Column(name = "viewed_by_to_user", nullable = false)
    private Boolean viewedByToUser = false;

    @Column(name = "viewed_by_from_user", nullable = false)
    private Boolean viewedByFromUser = false;
}
