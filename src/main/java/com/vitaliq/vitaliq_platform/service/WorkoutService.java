package com.vitaliq.vitaliq_platform.service;

import com.vitaliq.vitaliq_platform.dto.common.*;
import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseResponse;
import com.vitaliq.vitaliq_platform.dto.template.SetResponse;
import com.vitaliq.vitaliq_platform.dto.workout.*;
import com.vitaliq.vitaliq_platform.enums.SetContext;
import com.vitaliq.vitaliq_platform.kafka.WorkoutKafkaProducer;
import com.vitaliq.vitaliq_platform.event.WorkoutEvent;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.master.Exercise;
import com.vitaliq.vitaliq_platform.model.workout.*;
import com.vitaliq.vitaliq_platform.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final ExerciseSetRepository exerciseSetRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final WorkoutKafkaProducer workoutKafkaProducer;

    // ─── Helper: extract authenticated user from JWT ───────────────────────

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // ── POST /api/workouts ───────────────────────────────────────────────────

    @Transactional
    public WorkoutResponse logWorkout(LogWorkoutRequest request) {
        User user = getAuthenticatedUser();

        // 1. Save the Workout shell first — needs an ID before exercises can reference it
        Workout workout = new Workout();
        workout.setUser(user);
        workout.setName(request.getName());
        workout.setNotes(request.getNotes());
        workout.setStartTime(request.getStartTime());
        workout.setEndTime(request.getEndTime());
        workout.setTemplateId(request.getTemplateId());
        workoutRepository.save(workout);

        // 2. Build WorkoutExercises and their ExerciseSets
        double totalVolumeLbs = 0.0;
        int displayOrder = 0;
        int totalSets = 0;

        // LinkedHashSet — deduplicates muscle groups, preserves insertion order
        Set<String> muscleGroupsWorked = new LinkedHashSet<>();

        for (LogWorkoutExerciseRequest exReq : request.getExercises()) {
            Exercise exercise = exerciseRepository.findById(exReq.getExerciseId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Exercise not found: " + exReq.getExerciseId()));

            // Collect muscle group while we already have the Exercise entity in hand
            if (exercise.getPrimaryMuscleGroup() != null) {
                muscleGroupsWorked.add(exercise.getPrimaryMuscleGroup().name());
            }

            WorkoutExercise workoutExercise = new WorkoutExercise();
            workoutExercise.setWorkout(workout);
            workoutExercise.setExercise(exercise);
            workoutExercise.setOrderIndex(displayOrder++);
            workoutExercise.setNotes(exReq.getNotes());
            workoutExerciseRepository.save(workoutExercise);

            int setOrder = 0;
            for (CreateSetRequest setReq : exReq.getSets()) {
                ExerciseSet set = buildSet(setReq, workoutExercise, setOrder++);
                exerciseSetRepository.save(set);
                totalSets++;

                // Accumulate volume — only WeightedSets with non-null weight contribute
                if (set instanceof WeightedSet w
                        && w.getWeightLbs() != null
                        && w.getRepetitions() != null) {
                    totalVolumeLbs += w.getWeightLbs() * w.getRepetitions();
                }
            }
        }

        // 3. Write totalVolumeLbs back — null for cardio-only workouts
        workout.setTotalVolumeLbs(totalVolumeLbs > 0.0 ? totalVolumeLbs : null);
        workoutRepository.save(workout);

        // 4. Build and publish WorkoutEvent to Kafka
        // acks=0 — fire and forget. isIndexed flag is our reliability guarantee.
        WorkoutEvent event = WorkoutEvent.builder()
                .workoutId(workout.getId())
                .userId(user.getId())
                .workoutName(workout.getName())
                .templateId(workout.getTemplateId())
                .startTime(workout.getStartTime())
                .endTime(workout.getEndTime())
                .durationMinutes(ChronoUnit.MINUTES.between(
                        workout.getStartTime(), workout.getEndTime()))
                .totalVolumeLbs(workout.getTotalVolumeLbs())
                .exerciseCount(request.getExercises().size())
                .totalSets(totalSets)
                .muscleGroupsWorked(new ArrayList<>(muscleGroupsWorked))
                .occurredAt(LocalDateTime.now())
                .build();

        workoutKafkaProducer.publish(event);

        return toResponse(workout);
    }

    // ── GET /api/workouts ────────────────────────────────────────────────────

    public List<WorkoutResponse> getWorkoutHistory() {
        User user = getAuthenticatedUser();
        return workoutRepository.findByUserIdOrderByStartTimeDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── GET /api/workouts/{id} ───────────────────────────────────────────────

    public WorkoutResponse getWorkoutById(UUID id) {
        User user = getAuthenticatedUser();
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workout not found: " + id));
        assertOwnership(workout, user.getId());
        return toResponse(workout);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private ExerciseSet buildSet(CreateSetRequest req, WorkoutExercise workoutExercise, int setOrder) {
        if (req instanceof CreateWeightedSetRequest w) {
            WeightedSet set = new WeightedSet();
            set.setSetContext(SetContext.WORKOUT);
            set.setWorkoutExercise(workoutExercise);
            set.setSetNumber(setOrder);
            set.setWeightLbs(w.getWeightLbs());
            set.setRepetitions(w.getReps());
            set.setEquipmentType(w.getEquipmentType());
            set.setCompleted(true);  // sets logged after the fact are always completed
            return set;
        } else if (req instanceof CreateCardioSetRequest c) {
            CardioSet set = new CardioSet();
            set.setSetContext(SetContext.WORKOUT);
            set.setWorkoutExercise(workoutExercise);
            set.setSetNumber(setOrder);
            set.setDurationMinutes(c.getDurationMinutes());
            set.setDistanceMiles(c.getDistanceMiles());
            set.setCompleted(true);
            return set;
        }
        throw new IllegalArgumentException("Unknown set request type: " + req.getClass());
    }

    private void assertOwnership(Workout workout, UUID userId) {
        if (!workout.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Workout not found");  // intentionally vague
        }
    }

    private WorkoutResponse toResponse(Workout workout) {
        List<WorkoutExerciseResponse> exercises = workoutExerciseRepository
                .findByWorkoutIdOrderByOrderIndexAsc(workout.getId())
                .stream()
                .map(we -> WorkoutExerciseResponse.builder()
                        .id(we.getId())
                        .displayOrder(we.getOrderIndex())
                        .notes(we.getNotes())
                        .exercise(ExerciseResponse.from(we.getExercise()))
                        .sets(exerciseSetRepository
                                .findByWorkoutExerciseIdAndSetContext(we.getId(), SetContext.WORKOUT)
                                .stream()
                                .map(this::toSetResponse)
                                .toList())
                        .build())
                .toList();

        long durationMinutes = ChronoUnit.MINUTES.between(
                workout.getStartTime(), workout.getEndTime());

        return WorkoutResponse.builder()
                .id(workout.getId())
                .name(workout.getName())
                .notes(workout.getNotes())
                .startTime(workout.getStartTime())
                .endTime(workout.getEndTime())
                .durationMinutes(durationMinutes)
                .totalVolumeLbs(workout.getTotalVolumeLbs())
                .templateId(workout.getTemplateId())
                .exercises(exercises)
                .createdAt(workout.getCreatedAt())
                .build();
    }

    private SetResponse toSetResponse(ExerciseSet set) {
        if (set instanceof WeightedSet w) {
            return SetResponse.builder()
                    .id(w.getId()).setOrder(w.getSetNumber()).type("WEIGHTED")
                    .weightLbs(w.getWeightLbs()).reps(w.getRepetitions())
                    .equipmentType(w.getEquipmentType()).build();
        } else if (set instanceof CardioSet c) {
            return SetResponse.builder()
                    .id(c.getId()).setOrder(c.getSetNumber()).type("CARDIO")
                    .durationMinutes(c.getDurationMinutes()).distanceMiles(c.getDistanceMiles()).build();
        }
        throw new IllegalStateException("Unknown ExerciseSet subtype: " + set.getClass());
    }
}