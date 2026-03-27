package com.vitaliq.vitaliq_platform.dto.workout;

import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseResponse;
import com.vitaliq.vitaliq_platform.dto.template.SetResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter @Builder
public class WorkoutExerciseResponse {

    private UUID id;
    private Integer displayOrder;
    private String notes;
    private ExerciseResponse exercise;
    private List<SetResponse> sets;
}