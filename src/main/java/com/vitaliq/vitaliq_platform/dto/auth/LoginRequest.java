package com.vitaliq.vitaliq_platform.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}