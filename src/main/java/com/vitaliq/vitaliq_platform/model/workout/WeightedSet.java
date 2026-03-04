package com.vitaliq.vitaliq_platform.model.workout;

import com.vitaliq.vitaliq_platform.enums.EquipmentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weighted_sets")
@DiscriminatorValue("WEIGHTED")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WeightedSet extends ExerciseSet {

    private Double weightKg;

    @Enumerated(EnumType.STRING)
    private EquipmentType equipmentType;
}