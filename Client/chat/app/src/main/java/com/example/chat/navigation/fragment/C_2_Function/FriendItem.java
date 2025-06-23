package com.example.chat.navigation.fragment.C_2_Function;

import com.example.chat.model.User;
import net.sourceforge.pinyin4j.PinyinHelper;

public class FriendItem {
    private User user;
    private String initialLetter;

    public FriendItem(User user) {
        this.user = user;
        // 在构造函数中初始化 initialLetter，只获取拼音首字母
        this.initialLetter = calculateInitialLetter(user.getNickname());
    }

    public User getUser() {
        return user;
    }

    public String getInitialLetter() {
        return initialLetter;
    }

    private String calculateInitialLetter(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "#";
        }

        try {
            char firstChar = nickname.charAt(0);

            // 如果是英文字母，直接返回大写
            if (Character.isLetter(firstChar)) {
                String upperCase = String.valueOf(Character.toUpperCase(firstChar));
                // 确保是A-Z的英文字母
                if (upperCase.matches("[A-Z]")) {
                    return upperCase;
                }
            }

            // 如果是中文，获取拼音首字母
            if (Character.toString(firstChar).matches("[\\u4E00-\\u9FA5]")) {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(firstChar);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    // 获取拼音的第一个字母并转为大写
                    String firstLetter = pinyinArray[0].substring(0, 1).toUpperCase();
                    // 确保是A-Z的英文字母
                    if (firstLetter.matches("[A-Z]")) {
                        return firstLetter;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果不是A-Z的英文字母，返回#
        return "#";
    }
}