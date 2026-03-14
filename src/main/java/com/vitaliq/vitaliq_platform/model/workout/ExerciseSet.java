package com.vitaliq.vitaliq_platform.model.workout;

import com.vitaliq.vitaliq_platform.enums.SetContext;
import jakarta.persistence.*;
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
    @JoinColumn(name = "workout_exercise_id", nullable = true)  // nullable — template sets won't have this
    private WorkoutExercise workoutExercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_exercise_id", nullable = true)  // nullable — workout sets won't have this
    private TemplateExercise templateExercise;

    @Column(nullable = false)
    private Integer setNumber;

    @Column(nullable = false)
    private boolean isCompleted;

    private Integer repetitions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SetContext setContext;

    public abstract ExerciseSet deepCopy(TemplateExercise newParent, int setOrder);
}