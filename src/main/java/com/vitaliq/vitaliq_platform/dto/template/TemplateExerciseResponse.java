package com.vitaliq.vitaliq_platform.dto.template;

import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseResponse;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class TemplateExerciseResponse {
    private UUID id;
    private Integer displayOrder;
    private ExerciseResponse exercise;
    private List<SetResponse> sets;
}