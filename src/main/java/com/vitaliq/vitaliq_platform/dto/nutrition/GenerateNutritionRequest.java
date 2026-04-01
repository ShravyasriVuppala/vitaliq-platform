package com.vitaliq.vitaliq_platform.dto.nutrition;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GenerateNutritionRequest {

    // Optional — defaults to today if not provided
    private LocalDate planDate;
}