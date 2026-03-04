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

    @Field(type = FieldType.Date)
    private LocalDateTime startTime;

    @Field(type = FieldType.Date)
    private LocalDateTime endTime;

    @Field(type = FieldType.Double)
    private Double durationMinutes;

    @Field(type = FieldType.Keyword)
    private List<String> muscleGroupsWorked;

    @Field(type = FieldType.Integer)
    private Integer exerciseCount;

    @Field(type = FieldType.Integer)
    private Integer totalSets;

    @Field(type = FieldType.Date)
    private LocalDateTime occurredAt;

    @Field(type = FieldType.Double)
    private Double totalVolumeKg;
}
