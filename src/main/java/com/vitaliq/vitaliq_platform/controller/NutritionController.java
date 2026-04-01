package com.vitaliq.vitaliq_platform.controller;

import com.vitaliq.vitaliq_platform.dto.nutrition.GenerateNutritionRequest;
import com.vitaliq.vitaliq_platform.dto.nutrition.NutritionPlanResponse;
import com.vitaliq.vitaliq_platform.service.NutritionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionService nutritionService;

    // POST /api/nutrition/generate — generate AI meal plan
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public NutritionPlanResponse generatePlan(
            @Valid @RequestBody(required = false) GenerateNutritionRequest request) {
        // request is optional — default to empty request if body not provided
        if (request == null) {
            request = new GenerateNutritionRequest();
        }
        return nutritionService.generatePlan(request);
    }

    // GET /api/nutrition/active — get current active plan
    @GetMapping("/active")
    public NutritionPlanResponse getActivePlan() {
        return nutritionService.getActivePlan();
    }

    // GET /api/nutrition/history — get all past plans
    @GetMapping("/history")
    public List<NutritionPlanResponse> getPlanHistory() {
        return nutritionService.getPlanHistory();
    }
}