package com.vitaliq.vitaliq_platform.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class TemplateExerciseRequest {

    @NotNull
    private UUID exerciseId;

    @NotEmpty @Valid
    private List<CreateSetRequest> sets;
}