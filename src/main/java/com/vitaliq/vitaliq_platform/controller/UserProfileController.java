package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.user.*;
import com.vitaliq.vitaliq_platform.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    // ─── UserProfile ───────────────────────────────────────────────────────

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> createProfile(
            @RequestBody CreateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.createProfile(request));
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(request));
    }

    // ─── BodyMetricsLog ────────────────────────────────────────────────────

    @PostMapping("/body-metrics")
    public ResponseEntity<BodyMetricsResponse> logBodyMetrics(
            @RequestBody LogBodyMetricsRequest request) {
        return ResponseEntity.ok(userProfileService.logBodyMetrics(request));
    }

    @GetMapping("/body-metrics")
    public ResponseEntity<List<BodyMetricsResponse>> getBodyMetricsHistory() {
        return ResponseEntity.ok(userProfileService.getBodyMetricsHistory());
    }

    // ─── UserGoal ──────────────────────────────────────────────────────────

    @PostMapping("/goals")
    public ResponseEntity<GoalResponse> setGoal(
            @RequestBody SetGoalRequest request) {
        return ResponseEntity.ok(userProfileService.setGoal(request));
    }

    @GetMapping("/goals/active")
    public ResponseEntity<GoalResponse> getActiveGoal() {
        return ResponseEntity.ok(userProfileService.getActiveGoal());
    }
}