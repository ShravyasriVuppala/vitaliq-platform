package com.vitaliq.vitaliq_platform.model.workout;

import com.vitaliq.vitaliq_platform.enums.SetContext;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "exercise_sets")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
public abstract class ExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(nullable = false)
    private Integer setNumber;

    @Column(nullable = false)
    private boolean isCompleted;

    private Integer repetitions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SetContext setContext;
}