package com.vitaliq.vitaliq_platform.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}