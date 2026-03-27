package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.workout.LogWorkoutRequest;
import com.vitaliq.vitaliq_platform.dto.workout.WorkoutResponse;
import com.vitaliq.vitaliq_platform.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    // POST /api/workouts — log a completed workout
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse logWorkout(@Valid @RequestBody LogWorkoutRequest request) {
        return workoutService.logWorkout(request);
    }

    // GET /api/workouts — workout history for authenticated user
    @GetMapping
    public List<WorkoutResponse> getWorkoutHistory() {
        return workoutService.getWorkoutHistory();
    }

    // GET /api/workouts/{id} — single workout detail
    @GetMapping("/{id}")
    public WorkoutResponse getWorkoutById(@PathVariable UUID id) {
        return workoutService.getWorkoutById(id);
    }
}