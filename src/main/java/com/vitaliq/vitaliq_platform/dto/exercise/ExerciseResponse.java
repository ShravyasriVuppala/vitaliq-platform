package com.vitaliq.vitaliq_platform.dto.exercise;

import com.vitaliq.vitaliq_platform.enums.EquipmentType;
import com.vitaliq.vitaliq_platform.enums.ExerciseCategory;
import com.vitaliq.vitaliq_platform.enums.MuscleGroup;
import com.vitaliq.vitaliq_platform.model.master.Exercise;
import lombok.Data;

import java.util.UUID;

@Data
public class ExerciseResponse {
    private UUID id;
    private String name;
    private String description;
    private ExerciseCategory category;
    private MuscleGroup primaryMuscleGroup;
    private boolean isSystemExercise;
    private EquipmentType equipmentType;

    public static ExerciseResponse from(Exercise exercise) {
        ExerciseResponse response = new ExerciseResponse();
        response.setId(exercise.getId());
        response.setName(exercise.getName());
        response.setDescription(exercise.getDescription());
        response.setCategory(exercise.getCategory());
        response.setPrimaryMuscleGroup(exercise.getPrimaryMuscleGroup());
        response.setSystemExercise(exercise.isSystemExercise());
        response.setEquipmentType(exercise.getEquipmentType());
        return response;
    }
}