package com.vitaliq.vitaliq_platform.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MuscleGroupCount {

    private String muscleGroup;  // e.g. "CHEST", "BACK", "LEGS"
    private Long count;          // number of workouts that included this muscle group
}