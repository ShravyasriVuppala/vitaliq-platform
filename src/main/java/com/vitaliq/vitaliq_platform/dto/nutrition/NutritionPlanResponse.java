package com.vitaliq.vitaliq_platform.dto.nutrition;

import com.vitaliq.vitaliq_platform.enums.GoalType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class NutritionPlanResponse {

    private UUID id;
    private String planName;
    private LocalDate planDate;
    private boolean isActive;

    // Goal snapshot — what goal was active when this plan was generated
    private GoalType goalType;

    // Daily totals
    private Double totalCalories;
    private Double totalProteinG;
    private Double totalCarbsG;
    private Double totalFatsG;

    // Meals — BREAKFAST, LUNCH, DINNER, SNACK
    private List<MealResponse> meals;

    private LocalDateTime createdAt;
}