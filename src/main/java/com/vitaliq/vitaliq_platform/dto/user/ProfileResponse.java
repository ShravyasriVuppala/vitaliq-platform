package com.vitaliq.vitaliq_platform.dto.user;

import com.vitaliq.vitaliq_platform.enums.Gender;
import com.vitaliq.vitaliq_platform.enums.Lifestyle;
import com.vitaliq.vitaliq_platform.enums.WorkoutFrequency;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class ProfileResponse {
    private UUID id;
    private String email;
    private LocalDate dateOfBirth;
    private Gender gender;
    private Double heightCm;
    private Lifestyle lifestyle;
    private WorkoutFrequency workoutFrequency;
    private Set<String> healthConditions;
    private Set<String> allergies;
    private Set<String> dietaryPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}