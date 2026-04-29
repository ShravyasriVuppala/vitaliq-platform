package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.auth.ApiKeyRequest;
import com.vitaliq.vitaliq_platform.dto.auth.ApiKeyResponse;
import com.vitaliq.vitaliq_platform.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Generate a new API key for the authenticated user
     * POST /api/auth/api-keys/generate
     *
     * Request body: { "name": "MCP Server", "scope": "log_workouts" }
     * Response: 201 Created with the new API key (only shown once!)
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiKeyResponse> generateApiKey(
            @RequestBody ApiKeyRequest request
    ) {
        ApiKeyResponse response = apiKeyService.generateApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Revoke (delete) an API key
     * DELETE /api/auth/api-keys/{keyId}
     *
     * Security: Only the user who owns the key can delete it
     * Response: 204 No Content
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable UUID keyId
    ) {
        apiKeyService.revokeApiKey(keyId);
        return ResponseEntity.noContent().build();
    }
}