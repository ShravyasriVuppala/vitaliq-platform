package com.vitaliq.vitaliq_platform.dto.user;

import com.vitaliq.vitaliq_platform.enums.GoalType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GoalResponse {
    private UUID id;
    private GoalType goalType;
    private Double targetWeightKg;
    private boolean isActive;
    private LocalDateTime createdAt;
}