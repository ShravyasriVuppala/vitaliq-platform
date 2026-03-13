package com.vitaliq.vitaliq_platform.dto.exercise;

import com.vitaliq.vitaliq_platform.enums.ExerciseCategory;
import com.vitaliq.vitaliq_platform.enums.MuscleGroup;
import lombok.Data;

@Data
public class ExerciseSearchRequest {
    private ExerciseCategory category;
    private MuscleGroup muscleGroup;
}