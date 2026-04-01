package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.user.BodyMetricsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BodyMetricsLogRepository extends JpaRepository<BodyMetricsLog, UUID> {
    // Used by UserProfileService — full history
    List<BodyMetricsLog> findByUserIdOrderByLoggedAtDesc(UUID userId);

    // Used by NutritionService — latest entry only, no need to load all
    Optional<BodyMetricsLog> findFirstByUserIdOrderByLoggedAtDesc(UUID userId);
}