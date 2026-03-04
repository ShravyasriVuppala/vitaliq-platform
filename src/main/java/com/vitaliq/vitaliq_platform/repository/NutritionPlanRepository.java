package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.nutrition.NutritionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, UUID> {
    Optional<NutritionPlan> findByUserIdAndIsActiveTrue(UUID userId);
    List<NutritionPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
}