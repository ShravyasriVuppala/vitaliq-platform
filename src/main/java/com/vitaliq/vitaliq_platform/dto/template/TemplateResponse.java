package com.vitaliq.vitaliq_platform.dto.template;

import com.vitaliq.vitaliq_platform.enums.TemplateType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class TemplateResponse {
    private UUID id;
    private String name;
    private TemplateType type;
    private String createdByUsername;
    private UUID forkedFromId;
    private List<TemplateExerciseResponse> exercises;
    private LocalDateTime createdAt;
}