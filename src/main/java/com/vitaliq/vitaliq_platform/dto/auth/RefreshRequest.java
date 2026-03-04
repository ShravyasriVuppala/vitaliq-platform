package com.vitaliq.vitaliq_platform.dto.auth;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}