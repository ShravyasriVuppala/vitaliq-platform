package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.auth.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    // Find a key by its string value
    Optional<ApiKey> findByKey(String key);

    // Find a key by ID AND verify it belongs to a user (security check)
    Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);
}