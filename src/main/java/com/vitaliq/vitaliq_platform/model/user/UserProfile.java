package com.vitaliq.vitaliq_platform.model.user;

import com.vitaliq.vitaliq_platform.enums.Gender;
import com.vitaliq.vitaliq_platform.enums.Lifestyle;
import com.vitaliq.vitaliq_platform.enums.WorkoutFrequency;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.master.Allergy;
import com.vitaliq.vitaliq_platform.model.master.DietaryPreference;
import com.vitaliq.vitaliq_platform.model.master.HealthCondition;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Double heightCm;

    @Enumerated(EnumType.STRING)
    private Lifestyle lifestyle;

    @Enumerated(EnumType.STRING)
    private WorkoutFrequency workoutFrequency;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_health_conditions",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "health_condition_id")
    )
    private Set<HealthCondition> healthConditions = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_allergies",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "health_condition_id")
    )
    private Set<Allergy> allergies = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_dietary_preferences",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "dietary_preference_id")
    )
    private Set<DietaryPreference> dietaryPreferences = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}