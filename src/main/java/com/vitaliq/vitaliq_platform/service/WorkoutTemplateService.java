package com.vitaliq.vitaliq_platform.service;

import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseResponse;
import com.vitaliq.vitaliq_platform.dto.template.*;
import com.vitaliq.vitaliq_platform.enums.SetContext;
import com.vitaliq.vitaliq_platform.enums.TemplateType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final ExerciseSetRepository exerciseSetRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    // ─── Helper: extract authenticated user from JWT ───────────────────────

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // ── GET /api/templates ───────────────────────────────────────────────────

    public List<TemplateResponse> getTemplates() {
        User user = getAuthenticatedUser();
        List<WorkoutTemplate> templates = workoutTemplateRepository
                .findByTemplateTypeOrCreatedById(TemplateType.SYSTEM, user.getId());
        return templates.stream().map(this::toResponse).toList();
    }

    // ── POST /api/templates ──────────────────────────────────────────────────

    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        User user = getAuthenticatedUser();

        WorkoutTemplate template = new WorkoutTemplate();
        template.setName(request.getName());
        template.setTemplateType(TemplateType.USER);
        template.setCreatedBy(user);
        workoutTemplateRepository.save(template);

        buildAndSaveExercises(template, request.getExercises());

        return toResponse(workoutTemplateRepository.findById(template.getId()).orElseThrow());
    }

    // ── GET /api/templates/{id} ──────────────────────────────────────────────

    public TemplateResponse getTemplateById(UUID id) {
        User user = getAuthenticatedUser();
        WorkoutTemplate template = findTemplateOrThrow(id);
        assertVisibility(template, user.getId());
        return toResponse(template);
    }

    // ── PUT /api/templates/{id} ──────────────────────────────────────────────

    @Transactional
    public TemplateResponse updateTemplate(UUID id, CreateTemplateRequest request) {
        User user = getAuthenticatedUser();
        WorkoutTemplate template = findTemplateOrThrow(id);
        assertOwnership(template, user.getId());

        template.setName(request.getName());

        // Wipe and rebuild exercises — simpler than diffing
        exerciseSetRepository.deleteByTemplateExercise_WorkoutTemplate(template);
        templateExerciseRepository.deleteByWorkoutTemplate(template);

        buildAndSaveExercises(template, request.getExercises());

        return toResponse(workoutTemplateRepository.findById(id).orElseThrow());
    }

    // ── DELETE /api/templates/{id} ───────────────────────────────────────────

    @Transactional
    public void deleteTemplate(UUID id) {
        User user = getAuthenticatedUser();
        WorkoutTemplate template = findTemplateOrThrow(id);
        assertOwnership(template, user.getId());

        exerciseSetRepository.deleteByTemplateExercise_WorkoutTemplate(template);
        templateExerciseRepository.deleteByWorkoutTemplate(template);
        workoutTemplateRepository.delete(template);
    }

    // ── POST /api/templates/{id}/fork ────────────────────────────────────────

    @Transactional
    public TemplateResponse forkTemplate(UUID id) {
        User user = getAuthenticatedUser();
        WorkoutTemplate source = findTemplateOrThrow(id);
        assertVisibility(source, user.getId());

        WorkoutTemplate fork = new WorkoutTemplate();
        fork.setName(source.getName() + " (copy)");
        fork.setTemplateType(TemplateType.USER);
        fork.setCreatedBy(user);
        fork.setForkedFromId(source.getId());
        workoutTemplateRepository.save(fork);

        List<TemplateExercise> sourceExercises = templateExerciseRepository
                .findByWorkoutTemplateIdOrderByOrderIndexAsc(source.getId());
        for (TemplateExercise sourceEx : sourceExercises){
            TemplateExercise forkEx = new TemplateExercise();
            forkEx.setWorkoutTemplate(fork);
            forkEx.setExercise(sourceEx.getExercise());
            forkEx.setOrderIndex(sourceEx.getOrderIndex());
            forkEx.setPlannedSets(sourceEx.getPlannedSets());
            templateExerciseRepository.save(forkEx);

            int setOrder = 0;
            List<ExerciseSet> sourceSets = exerciseSetRepository
                    .findByTemplateExerciseIdAndSetContext(sourceEx.getId(), SetContext.TEMPLATE);
            for (ExerciseSet sourceSet : sourceSets) {
                ExerciseSet copiedSet = sourceSet.deepCopy(forkEx, setOrder++);
                exerciseSetRepository.save(copiedSet);
            }
        }

        return toResponse(workoutTemplateRepository.findById(fork.getId()).orElseThrow());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void buildAndSaveExercises(WorkoutTemplate template,
                                       List<TemplateExerciseRequest> exerciseRequests) {
        int displayOrder = 0;
        for (TemplateExerciseRequest exReq : exerciseRequests) {
            Exercise exercise = exerciseRepository.findById(exReq.getExerciseId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Exercise not found: " + exReq.getExerciseId()));

            TemplateExercise te = new TemplateExercise();
            te.setWorkoutTemplate(template);
            te.setExercise(exercise);
            te.setOrderIndex(displayOrder++);
            te.setPlannedSets(exReq.getSets().size());
            templateExerciseRepository.save(te);

            int setOrder = 0;
            for (CreateSetRequest setReq : exReq.getSets()) {
                ExerciseSet set = buildSet(setReq, te, setOrder++);
                exerciseSetRepository.save(set);
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

    private void assertOwnership(WorkoutTemplate template, UUID userId) {
        if (template.getTemplateType() == TemplateType.SYSTEM) {
            throw new AccessDeniedException("Cannot modify a SYSTEM template");
        }
        if (!template.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this template");
        }
    }


    private void assertVisibility(WorkoutTemplate template, UUID userId) {
        if (template.getTemplateType() == TemplateType.SYSTEM) return;
        if (!template.getCreatedBy().getId().equals(userId)) {
            throw new AccessDeniedException("Template not found"); // intentionally vague
        }
    }

    private WorkoutTemplate findTemplateOrThrow(UUID id) {
        return workoutTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + id));
    }

    private TemplateResponse toResponse(WorkoutTemplate t) {
        List<TemplateExerciseResponse> exercises = templateExerciseRepository
                .findByWorkoutTemplateIdOrderByOrderIndexAsc(t.getId()).stream()
                .map(te -> TemplateExerciseResponse.builder()
                        .id(te.getId())
                        .displayOrder(te.getOrderIndex())
                        .exercise(ExerciseResponse.from(te.getExercise()))
                        .sets(exerciseSetRepository
                                .findByTemplateExerciseIdAndSetContext(te.getId(), SetContext.TEMPLATE).stream().map(this::toSetResponse).toList())
                        .build())
                .toList();

        return TemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .type(t.getTemplateType())
                .createdByUsername(t.getCreatedBy() != null ? t.getCreatedBy().getEmail() : "system")
                .forkedFromId(t.getForkedFromId())
                .exercises(exercises)
                .createdAt(t.getCreatedAt())
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