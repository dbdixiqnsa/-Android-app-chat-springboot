package com.example.chat.repository;

import com.example.chat.model.SecurityQuestion;
import com.example.chat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {
    List<SecurityQuestion> findByUser(User user);
    void deleteByUserAndQuestion(User user, String question);
    SecurityQuestion findByUserAndQuestion(User user, String question);
}
