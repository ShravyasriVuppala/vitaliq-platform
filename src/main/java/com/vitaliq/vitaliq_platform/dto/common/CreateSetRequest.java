package com.vitaliq.vitaliq_platform.dto.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateWeightedSetRequest.class, name = "WEIGHTED"),
        @JsonSubTypes.Type(value = CreateCardioSetRequest.class,   name = "CARDIO")
})
public abstract class CreateSetRequest {
    // discriminator field is consumed by Jackson — no field needed here
}