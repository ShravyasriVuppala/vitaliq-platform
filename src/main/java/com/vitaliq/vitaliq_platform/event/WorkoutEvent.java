package com.vitaliq.vitaliq_platform.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutEvent {

    private UUID workoutId;
    private UUID userId;
    private String workoutName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime occurredAt;
}