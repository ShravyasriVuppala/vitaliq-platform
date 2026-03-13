package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.exercise.CreateExerciseRequest;
import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseResponse;
import com.vitaliq.vitaliq_platform.dto.exercise.ExerciseSearchRequest;
import com.vitaliq.vitaliq_platform.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping
    public ResponseEntity<List<ExerciseResponse>> searchExercises(
            @RequestBody(required = false) ExerciseSearchRequest request) {
        if (request == null) request = new ExerciseSearchRequest();
        return ResponseEntity.ok(exerciseService.searchExercises(request));
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> createExercise(
            @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.createExercise(request));
    }
}