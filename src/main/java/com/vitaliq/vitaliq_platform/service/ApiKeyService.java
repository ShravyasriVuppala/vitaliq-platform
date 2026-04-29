package com.vitaliq.vitaliq_platform.service;

import com.vitaliq.vitaliq_platform.dto.auth.ApiKeyRequest;
import com.vitaliq.vitaliq_platform.dto.auth.ApiKeyResponse;
import com.vitaliq.vitaliq_platform.model.auth.ApiKey;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.repository.ApiKeyRepository;
import com.vitaliq.vitaliq_platform.repository.UserRepository;
import com.vitaliq.vitaliq_platform.security.VitalIqUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    // ─── Helper: Extract authenticated user from JWT ───────────────────────

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    // ─── Generate API Key ──────────────────────────────────────────────────

    /**
     * Generate a new API key for the authenticated user
     */
    public ApiKeyResponse generateApiKey(ApiKeyRequest request) {
        // Validate input
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key name is required");
        }

        if (request.getScope() == null || request.getScope().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key scope is required");
        }

        // Get authenticated user
        User user = getAuthenticatedUser();

        // Generate secure key
        String apiKey = generateSecureKey();

        // Create entity
        ApiKey apiKeyEntity = new ApiKey();
        apiKeyEntity.setKey(apiKey);
        apiKeyEntity.setUser(user);
        apiKeyEntity.setName(request.getName());
        apiKeyEntity.setScope(request.getScope());
        apiKeyEntity.setIsActive(true);

        // Save to database
        ApiKey saved = apiKeyRepository.save(apiKeyEntity);

        // Convert to response (includes the key — only shown once!)
        return convertToResponse(saved);
    }

    // ─── Revoke API Key ────────────────────────────────────────────────────

    /**
     * Revoke (delete) an API key
     * Security: Only the user who owns the key can delete it
     */
    public void revokeApiKey(UUID keyId) {
        // Get authenticated user
        User user = getAuthenticatedUser();

        // Find the key AND verify user owns it (security!)
        var keyOpt = apiKeyRepository.findByIdAndUserId(keyId, user.getId());

        if (keyOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found");
        }

        // Delete the key
        apiKeyRepository.delete(keyOpt.get());
    }

    // ─── Helper Methods ────────────────────────────────────────────────────

    /**
     * Generate a secure random API key
     * Format: vk_test_[32 random alphanumeric characters]
     * Example: vk_test_aBc123DeF456gHi789jKl012mNo345pQr
     */
    private String generateSecureKey() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder key = new StringBuilder("vk_test_");

        for (int i = 0; i < 32; i++) {
            int index = (int) (Math.random() * characters.length());
            key.append(characters.charAt(index));
        }

        return key.toString();
    }

    /**
     * Convert ApiKey entity to response DTO
     */
    private ApiKeyResponse convertToResponse(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getKey(),
                apiKey.getName(),
                apiKey.getScope(),
                apiKey.getIsActive(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getExpiresAt()
        );
    }

    /**
     * Validate that the current API key has the required scope
     * Only checks if using API key auth (JWT auth has no scopes)
     */
    public void validateApiKeyScope(String requiredScope) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof VitalIqUserDetails userDetails) {
            ApiKey apiKey = userDetails.getApiKey();

            // If using API key auth, validate scope
            if (apiKey != null && !apiKey.hasScope(requiredScope)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "API key lacks required scope: " + requiredScope);
            }
        }
        // If JWT auth (no ApiKey), skip scope validation - JWT users have full access
    }

    /**
     * Get the current API key from authentication context
     * Returns null if using JWT auth
     */
    public ApiKey getCurrentApiKey() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof VitalIqUserDetails userDetails) {
            return userDetails.getApiKey();
        }
        return null;
    }
}