package com.vitaliq.vitaliq_platform.scheduler;

import com.vitaliq.vitaliq_platform.document.WorkoutDocument;
import com.vitaliq.vitaliq_platform.model.workout.Workout;
import com.vitaliq.vitaliq_platform.repository.WorkoutDocumentRepository;
import com.vitaliq.vitaliq_platform.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutIndexingRecoveryJob {

    private final WorkoutRepository workoutRepository;
    private final WorkoutDocumentRepository workoutDocumentRepository;

    // Runs every day at midnight — catches any workouts Kafka missed
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void recoverUnindexedWorkouts() {
        List<Workout> unindexed = workoutRepository.findByIsIndexedFalse();

        if (unindexed.isEmpty()) {
            log.info("Recovery job: no unindexed workouts found — ES is fully in sync");
            return;
        }

        log.info("Recovery job: found {} unindexed workout(s) — indexing now", unindexed.size());

        for (Workout workout : unindexed) {
            try {
                long durationMinutes = ChronoUnit.MINUTES.between(
                        workout.getStartTime(), workout.getEndTime());

                // Build document directly from PostgreSQL
                // exerciseCount, totalSets, muscleGroupsWorked are null here —
                // acceptable tradeoff for a recovery path. All workouts indexed
                // via Kafka will have these fields fully populated.
                WorkoutDocument document = WorkoutDocument.builder()
                        .id(workout.getId())
                        .userId(workout.getUser().getId())
                        .workoutName(workout.getName())
                        .templateId(workout.getTemplateId())
                        .startTime(workout.getStartTime())
                        .endTime(workout.getEndTime())
                        .durationMinutes(durationMinutes)
                        .totalVolumeLbs(workout.getTotalVolumeLbs())
                        .occurredAt(LocalDateTime.now())
                        .build();

                // Step 1 — index to ES first
                workoutDocumentRepository.save(document);

                // Step 2 — only then mark as indexed in PostgreSQL
                workoutRepository.markAsIndexed(workout.getId());

                log.info("Recovery job: successfully indexed workoutId={}", workout.getId());

            } catch (Exception e) {
                // Log and continue — don't fail the whole job for one bad record
                // This workout will be retried tomorrow night
                log.error("Recovery job: failed to index workoutId={} — will retry tomorrow. Error: {}",
                        workout.getId(), e.getMessage(), e);
            }
        }
    }
}