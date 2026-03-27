package com.vitaliq.vitaliq_platform.dto.common;

import com.vitaliq.vitaliq_platform.enums.EquipmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateWeightedSetRequest extends CreateSetRequest{

    private Double weightLbs;  // nullable — bodyweight exercises

    @NotNull @Min(1)
    private Integer reps;

    @NotNull
    private EquipmentType equipmentType;
}