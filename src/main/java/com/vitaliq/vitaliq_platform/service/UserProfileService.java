package com.vitaliq.vitaliq_platform.service;

import com.vitaliq.vitaliq_platform.dto.user.*;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.master.Allergy;
import com.vitaliq.vitaliq_platform.model.master.DietaryPreference;
import com.vitaliq.vitaliq_platform.model.master.HealthCondition;
import com.vitaliq.vitaliq_platform.model.user.BodyMetricsLog;
import com.vitaliq.vitaliq_platform.model.user.UserGoal;
import com.vitaliq.vitaliq_platform.model.user.UserProfile;
import com.vitaliq.vitaliq_platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BodyMetricsLogRepository bodyMetricsLogRepository;
    private final UserGoalRepository userGoalRepository;
    private final HealthConditionRepository healthConditionRepository;
    private final AllergyRepository allergyRepository;
    private final DietaryPreferenceRepository dietaryPreferenceRepository;

    // ─── Helper: extract authenticated user from JWT ───────────────────────

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // ─── UserProfile ───────────────────────────────────────────────────────

    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {
        User user = getAuthenticatedUser();

        // Prevent duplicate profiles
        if (userProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("Profile already exists for this user");
        }

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setHeightCm(request.getHeightCm());
        profile.setLifestyle(request.getLifestyle());
        profile.setWorkoutFrequency(request.getWorkoutFrequency());

        // Resolve master data by IDs
        if (request.getHealthConditionIds() != null) {
            Set<HealthCondition> conditions = new HashSet<>(
                    healthConditionRepository.findAllById(request.getHealthConditionIds()));
            profile.setHealthConditions(conditions);
        }

        if (request.getAllergyIds() != null) {
            Set<Allergy> allergies = new HashSet<>(
                    allergyRepository.findAllById(request.getAllergyIds()));
            profile.setAllergies(allergies);
        }

        if (request.getDietaryPreferenceIds() != null) {
            Set<DietaryPreference> preferences = new HashSet<>(
                    dietaryPreferenceRepository.findAllById(request.getDietaryPreferenceIds()));
            profile.setDietaryPreferences(preferences);
        }

        userProfileRepository.save(profile);
        return mapToProfileResponse(user, profile);
    }

    public ProfileResponse getProfile() {
        User user = getAuthenticatedUser();
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapToProfileResponse(user, profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getAuthenticatedUser();
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Update only non-null fields
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getHeightCm() != null) profile.setHeightCm(request.getHeightCm());
        if (request.getLifestyle() != null) profile.setLifestyle(request.getLifestyle());
        if (request.getWorkoutFrequency() != null) profile.setWorkoutFrequency(request.getWorkoutFrequency());

        if (request.getHealthConditionIds() != null) {
            profile.setHealthConditions(new HashSet<>(
                    healthConditionRepository.findAllById(request.getHealthConditionIds())));
        }
        if (request.getAllergyIds() != null) {
            profile.setAllergies(new HashSet<>(
                    allergyRepository.findAllById(request.getAllergyIds())));
        }
        if (request.getDietaryPreferenceIds() != null) {
            profile.setDietaryPreferences(new HashSet<>(
                    dietaryPreferenceRepository.findAllById(request.getDietaryPreferenceIds())));
        }

        userProfileRepository.save(profile);
        return mapToProfileResponse(user, profile);
    }

    // ─── BodyMetricsLog ────────────────────────────────────────────────────

    @Transactional
    public BodyMetricsResponse logBodyMetrics(LogBodyMetricsRequest request) {
        User user = getAuthenticatedUser();

        BodyMetricsLog log = new BodyMetricsLog();
        log.setUser(user);
        log.setWeightKg(request.getWeightKg());
        log.setBodyFatPercentage(request.getBodyFatPercentage());
        log.setLoggedAt(request.getLoggedAt() != null ? request.getLoggedAt() : LocalDate.now());

        bodyMetricsLogRepository.save(log);
        return mapToBodyMetricsResponse(log);
    }

    public List<BodyMetricsResponse> getBodyMetricsHistory() {
        User user = getAuthenticatedUser();
        return bodyMetricsLogRepository.findByUserIdOrderByLoggedAtDesc(user.getId())
                .stream()
                .map(this::mapToBodyMetricsResponse)
                .collect(Collectors.toList());
    }

    // ─── UserGoal ──────────────────────────────────────────────────────────

    @Transactional
    public GoalResponse setGoal(SetGoalRequest request) {
        User user = getAuthenticatedUser();

        // Deactivate current active goal if exists
        userGoalRepository.findByUserIdAndIsActiveTrue(user.getId())
                .ifPresent(existingGoal -> {
                    existingGoal.setActive(false);
                    userGoalRepository.save(existingGoal);
                });

        // Create new active goal
        UserGoal goal = new UserGoal();
        goal.setUser(user);
        goal.setGoalType(request.getGoalType());
        goal.setTargetWeightKg(request.getTargetWeightKg());
        goal.setActive(true);

        userGoalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    public GoalResponse getActiveGoal() {
        User user = getAuthenticatedUser();
        UserGoal goal = userGoalRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new RuntimeException("No active goal found"));
        return mapToGoalResponse(goal);
    }

    // ─── Mappers ───────────────────────────────────────────────────────────

    private ProfileResponse mapToProfileResponse(User user, UserProfile profile) {
        ProfileResponse response = new ProfileResponse();
        response.setId(profile.getId());
        response.setEmail(user.getEmail());
        response.setDateOfBirth(profile.getDateOfBirth());
        response.setGender(profile.getGender());
        response.setHeightCm(profile.getHeightCm());
        response.setLifestyle(profile.getLifestyle());
        response.setWorkoutFrequency(profile.getWorkoutFrequency());
        response.setHealthConditions(profile.getHealthConditions().stream()
                .map(HealthCondition::getName).collect(Collectors.toSet()));
        response.setAllergies(profile.getAllergies().stream()
                .map(Allergy::getName).collect(Collectors.toSet()));
        response.setDietaryPreferences(profile.getDietaryPreferences().stream()
                .map(DietaryPreference::getName).collect(Collectors.toSet()));
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }

    private BodyMetricsResponse mapToBodyMetricsResponse(BodyMetricsLog log) {
        BodyMetricsResponse response = new BodyMetricsResponse();
        response.setId(log.getId());
        response.setWeightKg(log.getWeightKg());
        response.setBodyFatPercentage(log.getBodyFatPercentage());
        response.setLoggedAt(log.getLoggedAt());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private GoalResponse mapToGoalResponse(UserGoal goal) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setGoalType(goal.getGoalType());
        response.setTargetWeightKg(goal.getTargetWeightKg());
        response.setActive(goal.isActive());
        response.setCreatedAt(goal.getCreatedAt());
        return response;
    }
}