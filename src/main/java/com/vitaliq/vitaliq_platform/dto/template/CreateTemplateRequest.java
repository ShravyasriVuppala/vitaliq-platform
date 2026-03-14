package com.vitaliq.vitaliq_platform.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class CreateTemplateRequest {

    @NotBlank
    private String name;

    @NotEmpty @Valid
    private List<TemplateExerciseRequest> exercises;
}