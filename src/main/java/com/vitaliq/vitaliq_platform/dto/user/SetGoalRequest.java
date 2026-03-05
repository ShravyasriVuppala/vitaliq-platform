package com.vitaliq.vitaliq_platform.dto.user;

import com.vitaliq.vitaliq_platform.enums.GoalType;
import lombok.Data;

@Data
public class SetGoalRequest {
    private GoalType goalType;
    private Double targetWeightKg;
}