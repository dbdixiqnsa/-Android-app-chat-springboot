package com.example.chat.navigation.fragment.C_3_Function.details;

import java.io.Serializable;

public class SecurityItem implements Serializable {
    private String question;
    private String answer;

    public SecurityItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public SecurityItem(String question) {
        this.question = question;
        this.answer = "";
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
