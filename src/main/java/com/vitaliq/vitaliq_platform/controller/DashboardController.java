package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.dashboard.DashboardSummaryResponse;
import com.vitaliq.vitaliq_platform.dto.dashboard.MuscleGroupCount;
import com.vitaliq.vitaliq_platform.dto.dashboard.VolumeDataPoint;
import com.vitaliq.vitaliq_platform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // GET /api/dashboard/summary — week + month stats
    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    // GET /api/dashboard/volume — volume per day, last 30 days
    @GetMapping("/volume")
    public List<VolumeDataPoint> getVolumeOverTime() {
        return dashboardService.getVolumeOverTime();
    }

    // GET /api/dashboard/muscle-groups — breakdown by muscle group this month
    @GetMapping("/muscle-groups")
    public List<MuscleGroupCount> getMuscleGroupBreakdown() {
        return dashboardService.getMuscleGroupBreakdown();
    }
}