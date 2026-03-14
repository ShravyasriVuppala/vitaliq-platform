package com.vitaliq.vitaliq_platform.model.workout;

import com.vitaliq.vitaliq_platform.enums.EquipmentType;
import com.vitaliq.vitaliq_platform.enums.SetContext;
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

    private Double weightLbs;

    @Enumerated(EnumType.STRING)
    private EquipmentType equipmentType;

    @Override
    public WeightedSet deepCopy(TemplateExercise newParent, int setOrder) {
        WeightedSet copy = new WeightedSet();
        copy.setSetContext(SetContext.TEMPLATE);
        copy.setTemplateExercise(newParent);
        copy.setSetNumber(setOrder);
        copy.setWeightLbs(this.weightLbs);
        copy.setRepetitions(this.getRepetitions());
        copy.setEquipmentType(this.equipmentType);
        return copy;
    }
}