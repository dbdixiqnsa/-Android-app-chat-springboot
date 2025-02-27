package com.example.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {

    @Value("${app.version}")
    private String version;

    public String getVersion() {
        return version;
    }
}