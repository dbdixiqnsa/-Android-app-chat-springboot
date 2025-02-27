package com.example.chat.repository;

import com.example.chat.model.FriendRequest;
import com.example.chat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    Optional<FriendRequest> findByFromUserAndToUser(User fromUser, User toUser);
    List<FriendRequest> findByFromUserOrToUser(User fromUser, User toUser);
    List<FriendRequest> findByFromUser(User fromUser);
}
