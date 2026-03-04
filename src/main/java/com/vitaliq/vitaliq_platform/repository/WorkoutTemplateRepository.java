package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.workout.WorkoutTemplate;
import com.vitaliq.vitaliq_platform.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, UUID> {
    List<WorkoutTemplate> findByTemplateType(TemplateType templateType);
    List<WorkoutTemplate> findByCreatedById(UUID userId);
}