package com.example.chat.service;

import com.example.chat.model.User;
import com.example.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public String registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return "用户名已存在";
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            return "邮箱已注册";
        }
        userRepository.save(user);
        return "注册成功";
    }

    public String loginUser(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return "账号或密码错误";
        }
        if (!user.getPassword().equals(password)) {
            return "账号或密码错误";
        }
        return "登录成功";
    }

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    //验证密码是否正确
    public boolean verifyPassword(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return false;
        }

        // 假设密码是明文存储的，实际应用中应使用哈希
        return user.getPassword().equals(password);
    }

    //更新用户密码
    public void updatePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            user.setPassword(newPassword); // 实际应用中应存储哈希
            userRepository.save(user);
        }
    }

}
