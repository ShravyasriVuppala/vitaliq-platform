package com.vitaliq.vitaliq_platform.service;

import com.vitaliq.vitaliq_platform.dto.common.CreateCardioSetRequest;
import com.vitaliq.vitaliq_platform.dto.common.CreateSetRequest;
import com.vitaliq.vitaliq_platform.dto.common.CreateWeightedSetRequest;
import com.vitaliq.vitaliq_platform.dto.workout.ExerciseModificationRequest;
import com.vitaliq.vitaliq_platform.dto.workout.LogWorkoutExerciseRequest;
import com.vitaliq.vitaliq_platform.dto.workout.LogWorkoutFromTemplateRequest;
import com.vitaliq.vitaliq_platform.dto.workout.WorkoutResponse;
import com.vitaliq.vitaliq_platform.dto.workout.LogWorkoutRequest;
import com.vitaliq.vitaliq_platform.enums.SetContext;
import com.vitaliq.vitaliq_platform.enums.TemplateType;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.workout.*;
import com.vitaliq.vitaliq_platform.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutFromTemplateService {

    private final WorkoutService workoutService;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final ExerciseSetRepository exerciseSetRepository;
    private final UserRepository userRepository;
    private final ApiKeyService apiKeyService;

    // ─── Helper: extract authenticated user ───────────────────────────────────

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // ── POST /api/workouts/from-template ──────────────────────────────────────

    @Transactional
    public WorkoutResponse logWorkoutFromTemplate(LogWorkoutFromTemplateRequest request) {
        apiKeyService.validateApiKeyScope("workouts");
        User user = getAuthenticatedUser();

        // 1. Load and authorize template
        WorkoutTemplate template = workoutTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Template not found: " + request.getTemplateId()));
        assertVisibility(template, user.getId());

        // 2. Resolve times — default to now / now+1h if omitted
        LocalDateTime start = request.getStartTime() != null
                ? request.getStartTime()
                : LocalDateTime.now();
        LocalDateTime end = request.getEndTime() != null
                ? request.getEndTime()
                : start.plusHours(1);

        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startTime must be before endTime");
        }

        // 3. Build modification map: exerciseId → replacement sets
        Map<UUID, List<CreateSetRequest>> modificationMap = buildModificationMap(request);

        // 4. Load template exercises in order
        List<TemplateExercise> templateExercises = templateExerciseRepository
                .findByWorkoutTemplateIdOrderByOrderIndexAsc(template.getId());

        // 5. Validate all modification exerciseIds exist in this template
        Set<UUID> templateExerciseIds = templateExercises.stream()
                .map(te -> te.getExercise().getId())
                .collect(Collectors.toSet());
        for (UUID modId : modificationMap.keySet()) {
            if (!templateExerciseIds.contains(modId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Exercise not found in template: " + modId);
            }
        }

        // 6. Merge template exercises with modifications into LogWorkoutExerciseRequests
        List<LogWorkoutExerciseRequest> exerciseRequests = new ArrayList<>();
        for (TemplateExercise te : templateExercises) {
            UUID exerciseId = te.getExercise().getId();

            List<CreateSetRequest> sets = modificationMap.containsKey(exerciseId)
                    ? modificationMap.get(exerciseId)
                    : toCreateSetRequests(te);

            LogWorkoutExerciseRequest exReq = new LogWorkoutExerciseRequest();
            exReq.setExerciseId(exerciseId);
            exReq.setSets(sets);
            exerciseRequests.add(exReq);
        }

        // 7. Build and delegate to WorkoutService
        LogWorkoutRequest workoutRequest = new LogWorkoutRequest();
        workoutRequest.setTemplateId(template.getId());
        workoutRequest.setName(template.getName());
        workoutRequest.setNotes(request.getNotes());
        workoutRequest.setStartTime(start);
        workoutRequest.setEndTime(end);
        workoutRequest.setExercises(exerciseRequests);

        WorkoutResponse response = workoutService.logWorkout(workoutRequest);

        // 8. Optionally update the template with the modifications
        if (Boolean.TRUE.equals(request.getUpdateTemplate()) && !modificationMap.isEmpty()) {
            updateTemplateWithModifications(template, modificationMap, templateExercises);
        }

        return response;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<UUID, List<CreateSetRequest>> buildModificationMap(LogWorkoutFromTemplateRequest request) {
        if (request.getExerciseModifications() == null || request.getExerciseModifications().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, List<CreateSetRequest>> map = new LinkedHashMap<>();
        for (ExerciseModificationRequest mod : request.getExerciseModifications()) {
            map.put(mod.getExerciseId(), mod.getModifiedSets());
        }
        return map;
    }

    private List<CreateSetRequest> toCreateSetRequests(TemplateExercise te) {
        return exerciseSetRepository
                .findByTemplateExerciseIdAndSetContext(te.getId(), SetContext.TEMPLATE)
                .stream()
                .map(this::toCreateSetRequest)
                .collect(Collectors.toList());
    }

    private CreateSetRequest toCreateSetRequest(ExerciseSet set) {
        if (set instanceof WeightedSet w) {
            CreateWeightedSetRequest req = new CreateWeightedSetRequest();
            req.setReps(w.getRepetitions());
            req.setWeightLbs(w.getWeightLbs());
            req.setEquipmentType(w.getEquipmentType());
            return req;
        } else if (set instanceof CardioSet c) {
            CreateCardioSetRequest req = new CreateCardioSetRequest();
            req.setDurationMinutes(c.getDurationMinutes());
            req.setDistanceMiles(c.getDistanceMiles());
            return req;
        }
        throw new IllegalStateException("Unknown ExerciseSet subtype: " + set.getClass());
    }

    private void updateTemplateWithModifications(WorkoutTemplate template,
                                                  Map<UUID, List<CreateSetRequest>> modificationMap,
                                                  List<TemplateExercise> templateExercises) {
        for (TemplateExercise te : templateExercises) {
            UUID exerciseId = te.getExercise().getId();
            if (!modificationMap.containsKey(exerciseId)) continue;

            // Replace sets: delete existing, save new
            List<ExerciseSet> existing = exerciseSetRepository
                    .findByTemplateExerciseIdAndSetContext(te.getId(), SetContext.TEMPLATE);
            exerciseSetRepository.deleteAll(existing);

            List<CreateSetRequest> newSets = modificationMap.get(exerciseId);
            te.setPlannedSets(newSets.size());
            templateExerciseRepository.save(te);

            int setOrder = 0;
            for (CreateSetRequest setReq : newSets) {
                exerciseSetRepository.save(buildSet(setReq, te, setOrder++));
            }
        }
    }

    private ExerciseSet buildSet(CreateSetRequest req, TemplateExercise te, int setOrder) {
        if (req instanceof CreateWeightedSetRequest w) {
            WeightedSet set = new WeightedSet();
            set.setSetContext(SetContext.TEMPLATE);
            set.setTemplateExercise(te);
            set.setSetNumber(setOrder);
            set.setWeightLbs(w.getWeightLbs());
            set.setRepetitions(w.getReps());
            set.setEquipmentType(w.getEquipmentType());
            return set;
        } else if (req instanceof CreateCardioSetRequest c) {
            CardioSet set = new CardioSet();
            set.setSetContext(SetContext.TEMPLATE);
            set.setTemplateExercise(te);
            set.setSetNumber(setOrder);
            set.setDurationMinutes(c.getDurationMinutes());
            set.setDistanceMiles(c.getDistanceMiles());
            return set;
        }
        throw new IllegalArgumentException("Unknown set request type: " + req.getClass());
    }

    private void assertVisibility(WorkoutTemplate template, UUID userId) {
        if (template.getTemplateType() == TemplateType.SYSTEM) return;
        if (!template.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("Template not found");  // intentionally vague
        }
    }
}