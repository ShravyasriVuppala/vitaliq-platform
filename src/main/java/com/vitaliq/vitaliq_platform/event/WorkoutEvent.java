package com.vitaliq.vitaliq_platform.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutEvent {

    private UUID workoutId;
    private UUID userId;
    private String workoutName;
    private UUID templateId;              // nullable — null if logged without a template
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMinutes;         // derived, carried so consumer needs no DB call
    private Double totalVolumeLbs;        // null if cardio-only
    private Integer exerciseCount;
    private Integer totalSets;
    private List<String> muscleGroupsWorked;  // empty for now — Phase 10 will enrich
    private LocalDateTime occurredAt;     // when the event was published
}