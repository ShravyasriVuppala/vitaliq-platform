package com.vitaliq.vitaliq_platform.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {
    private UUID id;
    private String key;                    // Only returned once on creation
    private String name;
    private String scope;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
}