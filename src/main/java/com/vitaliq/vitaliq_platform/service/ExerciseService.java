package com.vitaliq.vitaliq_platform.service;

import com.vitaliq.vitaliq_platform.dto.exercise.CreateExerciseRequest;
import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseResponse;
import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseSearchRequest;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.master.Exercise;
import com.vitaliq.vitaliq_platform.repository.ExerciseRepository;
import com.vitaliq.vitaliq_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final ApiKeyService apiKeyService;

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public List<ExerciseResponse> searchExercises(ExerciseSearchRequest request) {
        apiKeyService.validateApiKeyScope("workouts");
        User user = getAuthenticatedUser();

        List<Exercise> results = new ArrayList<>();

        if (request.getCategory() != null && request.getMuscleGroup() != null) {
            // Both filters provided — filter by category then muscle group
            results = exerciseRepository.findByCategory(request.getCategory())
                    .stream()
                    .filter(e -> e.getPrimaryMuscleGroup() == request.getMuscleGroup())
                    .filter(e -> e.isSystemExercise() ||
                            (e.getCreatedBy() != null &&
                                    e.getCreatedBy().getId().equals(user.getId())))
                    .collect(Collectors.toList());

        } else if (request.getCategory() != null) {
            // Only category filter
            results = exerciseRepository.findByCategory(request.getCategory())
                    .stream()
                    .filter(e -> e.isSystemExercise() ||
                            (e.getCreatedBy() != null &&
                                    e.getCreatedBy().getId().equals(user.getId())))
                    .collect(Collectors.toList());

        } else if (request.getMuscleGroup() != null) {
            // Only muscle group filter
            results = exerciseRepository.findByPrimaryMuscleGroup(request.getMuscleGroup())
                    .stream()
                    .filter(e -> e.isSystemExercise() ||
                            (e.getCreatedBy() != null &&
                                    e.getCreatedBy().getId().equals(user.getId())))
                    .collect(Collectors.toList());

        } else {
            // No filters — return all system + user's own exercises
            results.addAll(exerciseRepository.findByIsSystemExerciseTrue());
            results.addAll(exerciseRepository.findByCreatedByIdAndIsSystemExerciseFalse(user.getId()));
        }

        return results.stream()
                .map(this::mapToExerciseResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExerciseResponse createExercise(CreateExerciseRequest request) {
        apiKeyService.validateApiKeyScope("workouts");
        User user = getAuthenticatedUser();

        Exercise exercise = new Exercise();
        exercise.setName(request.getName());
        exercise.setDescription(request.getDescription());
        exercise.setCategory(request.getCategory());
        exercise.setPrimaryMuscleGroup(request.getPrimaryMuscleGroup());
        exercise.setSystemExercise(false);
        exercise.setCreatedBy(user);
        exercise.setEquipmentType(request.getEquipmentType());

        exerciseRepository.save(exercise);
        return mapToExerciseResponse(exercise);
    }

    private ExerciseResponse mapToExerciseResponse(Exercise exercise) {
        ExerciseResponse response = new ExerciseResponse();
        response.setId(exercise.getId());
        response.setName(exercise.getName());
        response.setDescription(exercise.getDescription());
        response.setCategory(exercise.getCategory());
        response.setPrimaryMuscleGroup(exercise.getPrimaryMuscleGroup());
        response.setSystemExercise(exercise.isSystemExercise());
        response.setEquipmentType(exercise.getEquipmentType());
        return response;
    }
}