package com.vitaliq.vitaliq_platform.model.master;

import com.vitaliq.vitaliq_platform.enums.ExerciseCategory;
import com.vitaliq.vitaliq_platform.enums.MuscleGroup;
import com.vitaliq.vitaliq_platform.model.auth.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exercises")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MuscleGroup primaryMuscleGroup;

    @Column(nullable = false)
    private boolean isSystemExercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}