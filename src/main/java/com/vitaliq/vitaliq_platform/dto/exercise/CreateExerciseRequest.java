package com.vitaliq.vitaliq_platform.dto.exercise;

import com.vitaliq.vitaliq_platform.enums.EquipmentType;
import com.vitaliq.vitaliq_platform.enums.ExerciseCategory;
import com.vitaliq.vitaliq_platform.enums.MuscleGroup;
import lombok.Data;

@Data
public class CreateExerciseRequest {
    private String name;
    private String description;
    private ExerciseCategory category;
    private MuscleGroup primaryMuscleGroup;
    private EquipmentType equipmentType;
}