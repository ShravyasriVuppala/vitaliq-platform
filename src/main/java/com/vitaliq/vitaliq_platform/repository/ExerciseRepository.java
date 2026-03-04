package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.master.Exercise;
import com.vitaliq.vitaliq_platform.enums.ExerciseCategory;
import com.vitaliq.vitaliq_platform.enums.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    List<Exercise> findByCategory(ExerciseCategory category);
    List<Exercise> findByPrimaryMuscleGroup(MuscleGroup muscleGroup);
    List<Exercise> findByIsSystemExerciseTrue();
    List<Exercise> findByCreatedByIdAndIsSystemExerciseFalse(UUID userId);
}