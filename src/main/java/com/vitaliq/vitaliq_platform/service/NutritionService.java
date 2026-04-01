package com.vitaliq.vitaliq_platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.ChatModel;
import com.vitaliq.vitaliq_platform.config.OpenAiConfig;
import com.vitaliq.vitaliq_platform.dto.nutrition.*;
import com.vitaliq.vitaliq_platform.model.auth.User;
import com.vitaliq.vitaliq_platform.model.nutrition.Meal;
import com.vitaliq.vitaliq_platform.model.nutrition.MealItem;
import com.vitaliq.vitaliq_platform.model.nutrition.NutritionPlan;
import com.vitaliq.vitaliq_platform.model.user.BodyMetricsLog;
import com.vitaliq.vitaliq_platform.model.user.UserGoal;
import com.vitaliq.vitaliq_platform.model.user.UserProfile;
import com.vitaliq.vitaliq_platform.model.workout.Workout;
import com.vitaliq.vitaliq_platform.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionService {

    private final NutritionPlanRepository nutritionPlanRepository;
    private final MealRepository mealRepository;
    private final MealItemRepository mealItemRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserGoalRepository userGoalRepository;
    private final BodyMetricsLogRepository bodyMetricsLogRepository;
    private final WorkoutRepository workoutRepository;
    private final ObjectMapper objectMapper;
    private final AiChatService aiChatService;

    // ─── Helper: extract authenticated user from JWT ───────────────────────

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // ── POST /api/nutrition/generate ─────────────────────────────────────────

    @Transactional
    public NutritionPlanResponse generatePlan(GenerateNutritionRequest request) {

        User user = getAuthenticatedUser();
        LocalDate planDate = request.getPlanDate() != null
                ? request.getPlanDate()
                : LocalDate.now();

        //log.debug("Calling AI service for userId={}", user.getId());

        // 1. Load context — everything GPT needs to generate a good plan
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User profile not found — please create a profile first"));

        UserGoal activeGoal = userGoalRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active goal found — please set a goal first"));

        // Latest body metrics — optional, graceful fallback if none logged
        BodyMetricsLog latestMetrics = bodyMetricsLogRepository
                .findFirstByUserIdOrderByLoggedAtDesc(user.getId())
                .orElse(null);  // null = no body metrics logged yet — graceful fallback

        // Last 3 workouts for context
        List<Workout> recentWorkouts = workoutRepository
                .findByUserIdOrderByStartTimeDesc(user.getId())
                .stream()
                .limit(3)
                .collect(Collectors.toList());

        // 2. Build the prompt
        String prompt = buildPrompt(profile, activeGoal, latestMetrics, recentWorkouts, planDate);
        log.debug("Generated nutrition prompt for userId={}", user.getId());

        // 3. Call AI provider
        String aiResponse;
        try {
            aiResponse = callAI(prompt);
            log.debug("Received AI response for userId={}", user.getId());
        } catch (Exception e) {
            log.error("AI service call failed for userId={} — {}",
                    user.getId(), e.getMessage(), e);  // full details in logs
            throw new RuntimeException(
                    "Failed to generate nutrition plan — AI service unavailable. Please try again.");
        }

        // 4. Parse JSON response
        JsonNode responseJson;
        try {
            responseJson = parseAiResponse(aiResponse);
        } catch (Exception e) {
            log.error("Failed to parse AI response for userId={} — response: {}", user.getId(), aiResponse, e);
            throw new RuntimeException("Failed to parse AI nutrition plan response. Please try again.");
        }

        // 5. Deactivate previous active plan
        nutritionPlanRepository.findByUserIdAndIsActiveTrue(user.getId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    nutritionPlanRepository.save(existing);
                });

        // 6. Build and save NutritionPlan
        NutritionPlan plan = new NutritionPlan();
        plan.setUser(user);
        plan.setUserGoal(activeGoal);
        plan.setPlanDate(planDate);
        plan.setPlanName(responseJson.path("planName").asText("Daily Nutrition Plan"));
        plan.setActive(true);
        plan.setAiPrompt(prompt);
        plan.setAiResponse(aiResponse);
        plan.setTotalCalories(responseJson.path("totalCalories").asDouble());
        plan.setTotalProteinG(responseJson.path("totalProteinG").asDouble());
        plan.setTotalCarbsG(responseJson.path("totalCarbsG").asDouble());
        plan.setTotalFatsG(responseJson.path("totalFatsG").asDouble());
        nutritionPlanRepository.save(plan);

        // 7. Build and save Meals + MealItems
        List<Meal> savedMeals = new ArrayList<>();
        JsonNode mealsJson = responseJson.path("meals");

        for (JsonNode mealJson : mealsJson) {
            Meal meal = new Meal();
            meal.setNutritionPlan(plan);
            meal.setMealType(com.vitaliq.vitaliq_platform.enums.MealType
                    .valueOf(mealJson.path("mealType").asText()));
            meal.setTotalCalories(mealJson.path("totalCalories").asDouble());
            meal.setTotalProteinG(mealJson.path("totalProteinG").asDouble());
            meal.setTotalCarbsG(mealJson.path("totalCarbsG").asDouble());
            meal.setTotalFatsG(mealJson.path("totalFatsG").asDouble());
            mealRepository.save(meal);

            JsonNode itemsJson = mealJson.path("items");
            for (JsonNode itemJson : itemsJson) {
                MealItem item = new MealItem();
                item.setMeal(meal);
                item.setFoodName(itemJson.path("foodName").asText());
                item.setQuantity(itemJson.path("quantity").asDouble());
                item.setUnit(itemJson.path("unit").asText());
                item.setCalories(itemJson.path("calories").asDouble());
                item.setProteinG(itemJson.path("proteinG").asDouble());
                item.setCarbsG(itemJson.path("carbsG").asDouble());
                item.setFatsG(itemJson.path("fatsG").asDouble());
                item.setUsdaFoodId("AI_GENERATED");  // no USDA lookup in Phase 11
                mealItemRepository.save(item);
            }

            savedMeals.add(meal);
        }

        return toResponse(plan, savedMeals);
    }

    // ── GET /api/nutrition/active ─────────────────────────────────────────────

    public NutritionPlanResponse getActivePlan() {
        User user = getAuthenticatedUser();
        NutritionPlan plan = nutritionPlanRepository
                .findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active nutrition plan — generate one first"));
        List<Meal> meals = mealRepository.findByNutritionPlanId(plan.getId());
        return toResponse(plan, meals);
    }

    // ── GET /api/nutrition/history ────────────────────────────────────────────

    public List<NutritionPlanResponse> getPlanHistory() {
        User user = getAuthenticatedUser();
        return nutritionPlanRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(plan -> {
                    List<Meal> meals = mealRepository.findByNutritionPlanId(plan.getId());
                    return toResponse(plan, meals);
                })
                .collect(Collectors.toList());
    }

    // ── Private: prompt builder ───────────────────────────────────────────────

    private String buildPrompt(UserProfile profile, UserGoal goal,
                               BodyMetricsLog metrics, List<Workout> recentWorkouts,
                               LocalDate planDate) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a professional nutritionist. Generate a personalized daily meal plan.\n\n");

        sb.append("User Profile:\n");
        sb.append("- Goal: ").append(goal.getGoalType()).append("\n");

        // Age derived from date of birth
        if (profile.getDateOfBirth() != null) {
            int age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
            sb.append("- Age: ").append(age).append("\n");
        }

        if (profile.getGender() != null) {
            sb.append("- Gender: ").append(profile.getGender()).append("\n");
        }

        if (profile.getHeightCm() != null) {
            sb.append("- Height: ").append(profile.getHeightCm()).append("cm\n");
        }

        // Current weight from latest body metrics
        if (metrics != null && metrics.getWeightKg() != null) {
            sb.append("- Current Weight: ").append(metrics.getWeightKg()).append("kg\n");
        }

        if (profile.getLifestyle() != null) {
            sb.append("- Lifestyle: ").append(profile.getLifestyle()).append("\n");
        }

        if (profile.getWorkoutFrequency() != null) {
            sb.append("- Workout Frequency: ").append(profile.getWorkoutFrequency()).append("\n");
        }

        // Dietary preferences
        if (!profile.getDietaryPreferences().isEmpty()) {
            String prefs = profile.getDietaryPreferences().stream()
                    .map(dp -> dp.getName())
                    .collect(Collectors.joining(", "));
            sb.append("- Dietary Preferences: ").append(prefs).append("\n");
        } else {
            sb.append("- Dietary Preferences: none\n");
        }

        // Allergies
        if (!profile.getAllergies().isEmpty()) {
            String allergies = profile.getAllergies().stream()
                    .map(a -> a.getName())
                    .collect(Collectors.joining(", "));
            sb.append("- Allergies: ").append(allergies).append("\n");
        } else {
            sb.append("- Allergies: none\n");
        }

        // Recent workout context
        sb.append("\nRecent Workout Activity (last ").append(recentWorkouts.size()).append(" workouts):\n");
        if (recentWorkouts.isEmpty()) {
            sb.append("- No recent workouts logged\n");
        } else {
            for (Workout w : recentWorkouts) {
                sb.append("- ").append(w.getName());
                if (w.getTotalVolumeLbs() != null) {
                    sb.append(": ").append(w.getTotalVolumeLbs()).append(" lbs volume");
                }
                sb.append(", duration: ").append(
                                java.time.temporal.ChronoUnit.MINUTES.between(
                                        w.getStartTime(), w.getEndTime()))
                        .append(" min\n");
            }
        }

        sb.append("\nPlan Date: ").append(planDate).append("\n\n");

        // Strict JSON format instruction
        sb.append("Return ONLY a valid JSON object with NO additional text, markdown, or explanation. ");
        sb.append("Use this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"planName\": \"string\",\n");
        sb.append("  \"totalCalories\": 0.0,\n");
        sb.append("  \"totalProteinG\": 0.0,\n");
        sb.append("  \"totalCarbsG\": 0.0,\n");
        sb.append("  \"totalFatsG\": 0.0,\n");
        sb.append("  \"meals\": [\n");
        sb.append("    {\n");
        sb.append("      \"mealType\": \"BREAKFAST\",\n");
        sb.append("      \"totalCalories\": 0.0,\n");
        sb.append("      \"totalProteinG\": 0.0,\n");
        sb.append("      \"totalCarbsG\": 0.0,\n");
        sb.append("      \"totalFatsG\": 0.0,\n");
        sb.append("      \"items\": [\n");
        sb.append("        {\n");
        sb.append("          \"foodName\": \"string\",\n");
        sb.append("          \"quantity\": 0.0,\n");
        sb.append("          \"unit\": \"string\",\n");
        sb.append("          \"calories\": 0.0,\n");
        sb.append("          \"proteinG\": 0.0,\n");
        sb.append("          \"carbsG\": 0.0,\n");
        sb.append("          \"fatsG\": 0.0\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("Include meals for BREAKFAST, LUNCH, DINNER, and SNACK.\n");
        sb.append("Each meal should have 2-4 food items with accurate nutritional data.\n");

        return sb.toString();
    }

    // ── Private: OpenAI call ──────────────────────────────────────────────────

    private String callAI(String prompt) {
        return aiChatService.generate(prompt);
    }

    // ── Private: parse AI response JSON ──────────────────────────────────────

    private JsonNode parseAiResponse(String response) {
        try {
            // Strip markdown code blocks if GPT wraps response in ```json ... ```
            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", response, e);
            throw new RuntimeException("Failed to parse AI meal plan response: " + e.getMessage());
        }
    }

    // ── Private: entity → response mappers ───────────────────────────────────

    private NutritionPlanResponse toResponse(NutritionPlan plan, List<Meal> meals) {
        List<MealResponse> mealResponses = meals.stream()
                .map(meal -> {
                    List<MealItem> items = mealItemRepository.findByMealId(meal.getId());
                    List<MealItemResponse> itemResponses = items.stream()
                            .map(item -> MealItemResponse.builder()
                                    .id(item.getId())
                                    .foodName(item.getFoodName())
                                    .quantity(item.getQuantity())
                                    .unit(item.getUnit())
                                    .calories(item.getCalories())
                                    .proteinG(item.getProteinG())
                                    .carbsG(item.getCarbsG())
                                    .fatsG(item.getFatsG())
                                    .build())
                            .collect(Collectors.toList());

                    return MealResponse.builder()
                            .id(meal.getId())
                            .mealType(meal.getMealType())
                            .totalCalories(meal.getTotalCalories())
                            .totalProteinG(meal.getTotalProteinG())
                            .totalCarbsG(meal.getTotalCarbsG())
                            .totalFatsG(meal.getTotalFatsG())
                            .items(itemResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return NutritionPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .planDate(plan.getPlanDate())
                .isActive(plan.isActive())
                .goalType(plan.getUserGoal().getGoalType())
                .totalCalories(plan.getTotalCalories())
                .totalProteinG(plan.getTotalProteinG())
                .totalCarbsG(plan.getTotalCarbsG())
                .totalFatsG(plan.getTotalFatsG())
                .meals(mealResponses)
                .createdAt(plan.getCreatedAt())
                .build();
    }
}