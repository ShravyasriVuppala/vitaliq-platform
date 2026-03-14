package com.vitaliq.vitaliq_platform.dto.template;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateCardioSetRequest extends CreateSetRequest {

    @NotNull @Positive
    private Double durationMinutes;

    private Double distanceMiles;  // nullable — treadmill with no distance tracking
}