package com.example.chat.dto;

import lombok.Data;
import java.util.List;

@Data
public class SecurityAnswerRequest {
    private String username;
    private List<SecurityAnswerDTO> answers;
}
