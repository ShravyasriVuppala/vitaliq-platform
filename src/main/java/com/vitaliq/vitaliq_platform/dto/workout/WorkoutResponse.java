package com.vitaliq.vitaliq_platform.dto.workout;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class WorkoutResponse {

    private UUID id;
    private String name;
    private String notes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMinutes;   // derived: endTime - startTime, never stored
    private Double totalVolumeLbs;  // null if cardio-only workout
    private UUID templateId;        // null if logged without a template
    private List<WorkoutExerciseResponse> exercises;
    private LocalDateTime createdAt;
}