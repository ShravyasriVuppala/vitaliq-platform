package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.workout.ExerciseSet;
import com.vitaliq.vitaliq_platform.enums.SetContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, UUID> {
    List<ExerciseSet> findByWorkoutExerciseIdAndSetContext(UUID workoutExerciseId, SetContext setContext);
}