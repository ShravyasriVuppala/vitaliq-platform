package com.vitaliq.vitaliq_platform.dto.workout;

import com.vitaliq.vitaliq_platform.dto.common.CreateSetRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter @Setter
public class LogWorkoutExerciseRequest {

    @NotNull
    private UUID exerciseId;

    private String notes;  // nullable — optional per-exercise note

    @NotEmpty @Valid
    private List<CreateSetRequest> sets;
}