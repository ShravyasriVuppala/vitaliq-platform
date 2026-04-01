package com.vitaliq.vitaliq_platform.dto.nutrition;

import com.vitaliq.vitaliq_platform.enums.MealType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MealResponse {

    private UUID id;
    private MealType mealType;       // BREAKFAST, LUNCH, DINNER, SNACK
    private Double totalCalories;
    private Double totalProteinG;
    private Double totalCarbsG;
    private Double totalFatsG;
    private List<MealItemResponse> items;
}