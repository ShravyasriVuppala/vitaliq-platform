package com.vitaliq.vitaliq_platform.document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(indexName = "workouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutDocument {

    @Id
    private UUID id;

    @Field(type = FieldType.Keyword)
    private UUID userId;

    @Field(type = FieldType.Text)
    private String workoutName;

    @Field(type = FieldType.Keyword)
    private UUID templateId;  // nullable

    @Field(type = FieldType.Date)
    private LocalDateTime startTime;

    @Field(type = FieldType.Date)
    private LocalDateTime endTime;

    @Field(type = FieldType.Long)
    private Long durationMinutes;  // fixed: was Double

    @Field(type = FieldType.Double)
    private Double totalVolumeLbs;  // renamed from totalVolumeKg; null if cardio-only

    @Field(type = FieldType.Integer)
    private Integer exerciseCount;

    @Field(type = FieldType.Integer)
    private Integer totalSets;

    @Field(type = FieldType.Keyword)
    private List<String> muscleGroupsWorked;  // each value independently searchable in ES

    @Field(type = FieldType.Date)
    private LocalDateTime occurredAt;
}