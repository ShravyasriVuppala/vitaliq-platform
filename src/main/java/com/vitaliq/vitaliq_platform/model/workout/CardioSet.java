package com.vitaliq.vitaliq_platform.model.workout;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cardio_sets")
@DiscriminatorValue("CARDIO")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CardioSet extends ExerciseSet {

    private Double durationMinutes;

    private Double distanceMiles;
}