package com.vitaliq.vitaliq_platform.dto.workout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class LogWorkoutFromTemplateRequest {

    @NotNull
    private UUID templateId;

    private LocalDateTime startTime;  // optional — defaults to now

    private LocalDateTime endTime;    // optional — defaults to 1 hour after start

    @Valid
    private List<ExerciseModificationRequest> exerciseModifications;  // optional — empty means log as-is

    private String notes;

    @NotNull
    private Boolean updateTemplate;  // required — forces caller to make explicit decision
}