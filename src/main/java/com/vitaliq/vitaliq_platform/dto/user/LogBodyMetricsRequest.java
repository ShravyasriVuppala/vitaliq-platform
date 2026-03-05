package com.vitaliq.vitaliq_platform.dto.user;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LogBodyMetricsRequest {
    private Double weightKg;
    private Double bodyFatPercentage;
    private LocalDate loggedAt;
}