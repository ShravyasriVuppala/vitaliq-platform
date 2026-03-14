package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.template.CreateTemplateRequest;
import com.vitaliq.vitaliq_platform.dto.template.TemplateResponse;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.service.WorkoutTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class WorkoutTemplateController {

    private final WorkoutTemplateService workoutTemplateService;

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getTemplates() {
        return ResponseEntity.ok(workoutTemplateService.getTemplates());
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutTemplateService.createTemplate(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(workoutTemplateService.getTemplateById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(workoutTemplateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID id) {
        workoutTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fork")
    public ResponseEntity<TemplateResponse> forkTemplate(
            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutTemplateService.forkTemplate(id));
    }
}