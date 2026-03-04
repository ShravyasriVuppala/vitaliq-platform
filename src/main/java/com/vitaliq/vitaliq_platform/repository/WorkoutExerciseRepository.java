package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.workout.WorkoutExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {
    List<WorkoutExercise> findByWorkoutIdOrderByOrderIndexAsc(UUID workoutId);
}