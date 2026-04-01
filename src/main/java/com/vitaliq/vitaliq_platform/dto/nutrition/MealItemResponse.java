package com.vitaliq.vitaliq_platform.dto.nutrition;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MealItemResponse {

    private UUID id;
    private String foodName;
    private Double quantity;
    private String unit;
    private Double calories;
    private Double proteinG;
    private Double carbsG;
    private Double fatsG;
}