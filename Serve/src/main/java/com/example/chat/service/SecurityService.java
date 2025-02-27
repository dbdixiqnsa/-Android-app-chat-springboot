package com.example.chat.service;

import com.example.chat.dto.SecurityAnswerDTO;
import com.example.chat.model.SecurityQuestion;
import com.example.chat.model.User;
import com.example.chat.repository.SecurityQuestionRepository;
import com.example.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SecurityService {

    @Autowired
    private SecurityQuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    public List<SecurityQuestion> getQuestionsByUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        return questionRepository.findByUser(user);
    }

    public void addQuestions(String username, List<SecurityQuestion> questions) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        // 检查密保问题是否重复
        List<String> existingQuestions = questionRepository.findByUser(user)
                .stream()
                .map(SecurityQuestion::getQuestion)
                .toList();

        for (SecurityQuestion question : questions) {
            if (existingQuestions.contains(question.getQuestion())) {
                throw new IllegalArgumentException("密保问题重复: " + question.getQuestion());
            }
            question.setUser(user);
            questionRepository.save(question);
        }
    }

    public void updateQuestion(String username, String oldQuestion, SecurityQuestion newQuestion) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        questionRepository.deleteByUserAndQuestion(user, oldQuestion);
        newQuestion.setUser(user);
        questionRepository.save(newQuestion);
    }

    public void deleteQuestion(String username, String question) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        SecurityQuestion securityQuestion = questionRepository.findByUserAndQuestion(user, question);
        if (securityQuestion != null) {
            questionRepository.delete(securityQuestion);
        } else {
            throw new IllegalArgumentException("密保问题不存在");
        }
    }

    public boolean verifyAnswers(String username, List<SecurityAnswerDTO> answers) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        List<SecurityQuestion> storedQuestions = questionRepository.findByUser(user);

        int correctCount = 0;

        for (SecurityAnswerDTO answer : answers) {
            String question = answer.getQuestion();
            String providedAnswer = answer.getAnswer();

            for (SecurityQuestion storedQuestion : storedQuestions) {
                if (storedQuestion.getQuestion().equals(question)) {
                    if (storedQuestion.getAnswer().equals(providedAnswer)) {
                        correctCount++;
                        break;
                    }
                }
            }
        }
        return correctCount == 3;
    }

}
