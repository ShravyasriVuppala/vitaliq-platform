package com.vitaliq.vitaliq_platform.dto.user;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BodyMetricsResponse {
    private UUID id;
    private Double weightKg;
    private Double bodyFatPercentage;
    private LocalDate loggedAt;
    private LocalDateTime createdAt;
}