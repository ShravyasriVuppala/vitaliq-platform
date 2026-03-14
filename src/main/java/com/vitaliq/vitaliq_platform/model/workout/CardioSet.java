package com.vitaliq.vitaliq_platform.model.workout;

import com.vitaliq.vitaliq_platform.enums.SetContext;
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

    @Override
    public CardioSet deepCopy(TemplateExercise newParent, int setOrder) {
        CardioSet copy = new CardioSet();
        copy.setSetContext(SetContext.TEMPLATE);
        copy.setTemplateExercise(newParent);
        copy.setSetNumber(setOrder);
        copy.setDurationMinutes(this.durationMinutes);
        copy.setDistanceMiles(this.distanceMiles);
        return copy;
    }
}