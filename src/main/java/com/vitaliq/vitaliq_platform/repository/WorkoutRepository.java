package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.workout.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    // Used by WorkoutService — workout history ordered most recent first
    List<Workout> findByUserIdOrderByStartTimeDesc(UUID userId);

    // Used by WorkoutService — workouts within a date range
    List<Workout> findByUserIdAndStartTimeBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    // Used by daily recovery job — finds all workouts Kafka failed to index
    List<Workout> findByIsIndexedFalse();

    // Used by Kafka Consumer — marks workout as indexed after ES confirms
    @Modifying
    @Query("UPDATE Workout w SET w.isIndexed = true WHERE w.id = :id")
    void markAsIndexed(@Param("id") UUID id);
}