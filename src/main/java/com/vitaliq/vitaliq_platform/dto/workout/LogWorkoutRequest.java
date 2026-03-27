package com.vitaliq.vitaliq_platform.dto.workout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class LogWorkoutRequest {

    private UUID templateId;  // nullable — null means logged without a template

    @NotBlank
    private String name;

    private String notes;  // nullable

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotEmpty @Valid
    private List<LogWorkoutExerciseRequest> exercises;
}