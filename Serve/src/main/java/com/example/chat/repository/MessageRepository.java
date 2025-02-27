package com.example.chat.repository;

import com.example.chat.model.Message;
import com.example.chat.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 获取两个用户之间的所有消息，按时间顺序排序
    List<Message> findBySenderAndReceiverOrderByTimestamp(User sender, User receiver);

    // 获取两个用户之间的最新一条消息
    Message findFirstBySenderAndReceiverOrderByTimestampDesc(User sender, User receiver);

    // 获取与某用户相关的所有消息
    List<Message> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);

    // 获取两个用户之间的所有消息，无论谁是发送者或接收者
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.timestamp")
    List<Message> findMessagesBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    // 查找两个用户之间未读的消息
    @Query("SELECT m FROM Message m WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    List<Message> findUnreadMessagesBetweenUsers(@Param("sender") User sender, @Param("receiver") User receiver);

    // 按发送者分组统计未读消息数量
    @Query("SELECT m.sender.username, COUNT(m) FROM Message m WHERE m.receiver = :user AND m.isRead = false GROUP BY m.sender.username")
    List<Object[]> countUnreadMessagesGroupBySender(@Param("user") User user);

    // 获取两个用户之间的最新一条消息（修改后的方法）
    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.timestamp DESC")
    List<Message> findLatestMessageBetweenUsers(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);
}
