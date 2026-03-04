package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.workout.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    List<Workout> findByUserIdOrderByStartTimeDesc(UUID userId);
    List<Workout> findByUserIdAndStartTimeBetween(UUID userId, LocalDateTime from, LocalDateTime to);
}