package com.vitaliq.vitaliq_platform.dto.user;

import com.vitaliq.vitaliq_platform.enums.Gender;
import com.vitaliq.vitaliq_platform.enums.Lifestyle;
import com.vitaliq.vitaliq_platform.enums.WorkoutFrequency;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
public class UpdateProfileRequest {
    private LocalDate dateOfBirth;
    private Gender gender;
    private Double heightCm;
    private Lifestyle lifestyle;
    private WorkoutFrequency workoutFrequency;
    private Set<UUID> healthConditionIds;
    private Set<UUID> allergyIds;
    private Set<UUID> dietaryPreferenceIds;
}