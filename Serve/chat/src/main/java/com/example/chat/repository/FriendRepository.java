package com.example.chat.repository;

import com.example.chat.model.Friend;
import com.example.chat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUser(User user);
    List<Friend> findByFriend(User friend);
    Optional<Friend> findByUserAndFriend(User user, User friend);

    /**
     * 检查两个用户之间是否已经存在好友关系
     *
     * @param user   当前用户
     * @param friend 目标好友
     * @return 如果存在，则返回 true，否则返回 false
     */
    boolean existsByUserAndFriend(User user, User friend);
}
