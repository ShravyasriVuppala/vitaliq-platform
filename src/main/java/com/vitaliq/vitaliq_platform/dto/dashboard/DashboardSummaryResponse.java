package com.vitaliq.vitaliq_platform.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSummaryResponse {

    // This week stats
    private Long workoutsThisWeek;
    private Double totalVolumeLbsThisWeek;   // null if no weighted workouts
    private Long totalDurationMinutesThisWeek;
    private Long avgDurationMinutesThisWeek;  // null if no workouts

    // This month stats
    private Long workoutsThisMonth;
    private Double totalVolumeLbsThisMonth;  // null if no weighted workouts
    private Long totalDurationMinutesThisMonth;
    private Long avgDurationMinutesThisMonth; // null if no workouts
}