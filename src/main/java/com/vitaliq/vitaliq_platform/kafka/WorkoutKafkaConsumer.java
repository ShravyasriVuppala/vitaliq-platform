package com.vitaliq.vitaliq_platform.kafka;

import com.vitaliq.vitaliq_platform.document.WorkoutDocument;
import com.vitaliq.vitaliq_platform.event.WorkoutEvent;
import com.vitaliq.vitaliq_platform.repository.WorkoutDocumentRepository;
import com.vitaliq.vitaliq_platform.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutKafkaConsumer {

    private final WorkoutDocumentRepository workoutDocumentRepository;
    private final WorkoutRepository workoutRepository;

    @KafkaListener(topics = "workout-events", groupId = "vitaliq-group") // tells sprint to run this method everytime a new message arrives at this kafka topic
    @Transactional
    public void consume(WorkoutEvent event) {
        log.debug("Received WorkoutEvent for workoutId={}", event.getWorkoutId());
        try {
            // Step 1 — build WorkoutDocument from the event (no DB call needed)
            WorkoutDocument document = WorkoutDocument.builder()
                    .id(event.getWorkoutId())
                    .userId(event.getUserId().toString())
                    .workoutName(event.getWorkoutName())
                    .templateId(event.getTemplateId())
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .durationMinutes(event.getDurationMinutes())
                    .totalVolumeLbs(event.getTotalVolumeLbs())
                    .exerciseCount(event.getExerciseCount())
                    .totalSets(event.getTotalSets())
                    .muscleGroupsWorked(event.getMuscleGroupsWorked())
                    .occurredAt(event.getOccurredAt())
                    .build();

            // Step 2 — index into Elasticsearch first
            workoutDocumentRepository.save(document);
            log.debug("Indexed WorkoutDocument to ES for workoutId={}", event.getWorkoutId());

            // Step 3 — only after ES confirms, mark as indexed in PostgreSQL
            workoutRepository.markAsIndexed(event.getWorkoutId());
            log.debug("Marked workoutId={} as indexed in PostgreSQL", event.getWorkoutId());

        } catch (Exception e) {
            // Rethrow so Kafka does NOT commit the offset — message will be redelivered
            // isIndexed stays false so daily recovery job covers it if retries are exhausted
            log.error("Failed to index workoutId={} — Kafka will retry. Error: {}",
                    event.getWorkoutId(), e.getMessage(), e);
            throw e;
        }
    }
}