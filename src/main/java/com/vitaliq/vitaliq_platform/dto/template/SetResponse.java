package com.vitaliq.vitaliq_platform.dto.template;

import com.vitaliq.vitaliq_platform.enums.EquipmentType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder
public class SetResponse {
    private UUID id;
    private Integer setOrder;
    private String type;             // "WEIGHTED" or "CARDIO"

    // WeightedSet fields
    private Double weightLbs;
    private Integer reps;
    private EquipmentType equipmentType;

    // CardioSet fields
    private Double durationMinutes;
    private Double distanceMiles;
}