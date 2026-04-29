package com.vitaliq.vitaliq_platform.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyRequest {
    private String name;    // e.g., "MCP Server"
    private String scope;   // e.g., "log_workouts" or "log_workouts,view_dashboard"
}