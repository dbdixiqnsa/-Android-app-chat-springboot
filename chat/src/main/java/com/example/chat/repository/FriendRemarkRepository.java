package com.example.chat.repository;

import com.example.chat.model.FriendRemark;
import com.example.chat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendRemarkRepository extends JpaRepository<FriendRemark, Long> {
    Optional<FriendRemark> findByUserAndFriend(User user, User friend);
}