package com.vitaliq.vitaliq_platform.model.nutrition;

import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.user.UserGoal;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nutrition_plans")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class NutritionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_goal_id", nullable = false)
    private UserGoal userGoal;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column
    private String planName;

    @Column(nullable = false)
    private boolean isActive;

    @Column(columnDefinition = "TEXT")
    private String aiPrompt;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    private Double totalCalories;
    private Double totalProteinG;
    private Double totalCarbsG;
    private Double totalFatsG;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}