package com.example.backend_meal.nutrition.controller;

import com.example.backend_meal.ai.AIService;
import com.example.backend_meal.nutrition.dto.MealPlanResponseDTO;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.service.MealPlanService;
import com.example.backend_meal.nutrition.service.NutritionCalculatorService;
import com.example.backend_meal.nutrition.service.NutritionProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for the Nutrition module.
 * All endpoints are under /api/nutrition.
 * Angular calls /nutrition/* which is proxied to /api/nutrition/*.
 *
 * API KEY IS NEVER INCLUDED IN ANY RESPONSE.
 */
@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    private static final Logger log = LoggerFactory.getLogger(NutritionController.class);

    private final AIService aiService;
    private final NutritionCalculatorService calculator;
    private final NutritionProfileService profileService;
    private final MealPlanService mealPlanService;

    public NutritionController(AIService aiService,
                               NutritionCalculatorService calculator,
                               NutritionProfileService profileService,
                               MealPlanService mealPlanService) {
        this.aiService = aiService;
        this.calculator = calculator;
        this.profileService = profileService;
        this.mealPlanService = mealPlanService;
    }

    @GetMapping("/ai-status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        boolean isReal = aiService.isRealAI();
        String provider = aiService.getProviderName();
        String message = isReal
            ? "Fournisseur IA générative actif : " + provider
            : "Aucune clé API IA configurée. Mode développement algorithmique actif — " +
              "les repas ne sont PAS générés par une IA générative.";

        return ResponseEntity.ok(Map.of(
            "isRealAI",     isReal,
            "providerName", provider,
            "message",      message
        ));
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<NutritionProfile>> getAllProfiles() {
        return ResponseEntity.ok(profileService.findAll());
    }

    @GetMapping("/profiles/{id}")
    public ResponseEntity<?> getProfileById(@PathVariable Long id) {
        return profileService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/profiles")
    public ResponseEntity<NutritionProfile> createProfile(@RequestBody NutritionProfile profile) {
        NutritionProfile saved = profileService.createOrUpdate(profile);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/profiles/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody NutritionProfile updates) {
        try {
            NutritionProfile saved = profileService.update(id, updates);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getLegacyProfile(@RequestParam(required = false) String userId,
                                              @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String uid = resolveUserId(userId, headerUserId, null);
        Optional<NutritionProfile> profile = profileService.findByUserId(uid);
        if (profile.isPresent()) {
            log.info("GET /api/nutrition/profile found persisted profile id={}", profile.get().getId());
        } else {
            log.info("GET /api/nutrition/profile found no persisted profile");
        }
        return profile
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<NutritionProfile> saveLegacyProfile(
            @RequestBody NutritionProfile profileData,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        if (profileData.getUserId() == null || profileData.getUserId().isBlank()) {
            profileData.setUserId(resolveUserId(userId, headerUserId, profileData.getUserId()));
        }
        NutritionProfile saved = profileService.createOrUpdate(profileData);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/profile")
    public ResponseEntity<NutritionProfile> saveLegacyProfilePost(
            @RequestBody NutritionProfile profileData,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        return saveLegacyProfile(profileData, userId, headerUserId);
    }

    @PostMapping("/calculate")
    public ResponseEntity<?> calculate(@RequestBody NutritionProfile profile) {
        try {
            NutritionProfile result = calculator.calculate(profile);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping({"/meal-plan/generate", "/meal-plan/regenerate"})
    public ResponseEntity<?> generateMealPlan(
            @RequestBody(required = false) NutritionProfile profile,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            String uid = resolveUserId(userId, headerUserId, profile != null ? profile.getUserId() : null);
            MealPlanResponseDTO result = mealPlanService.generateAndSave(profile, uid);
            return ResponseEntity.status(201).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("[NutritionController] Erreur génération : " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Échec de la génération du programme alimentaire : " + e.getMessage()
            ));
        }
    }

    @GetMapping("/meal-plans")
    public ResponseEntity<List<MealPlanResponseDTO>> getAllMealPlans() {
        return ResponseEntity.ok(mealPlanService.getAllMealPlans());
    }

    @GetMapping("/meal-plans/{id}")
    public ResponseEntity<?> getMealPlanById(@PathVariable Long id) {
        return mealPlanService.getMealPlanById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profiles/{id}/meal-plans")
    public ResponseEntity<List<MealPlanResponseDTO>> getMealPlansByProfile(@PathVariable Long id) {
        return ResponseEntity.ok(mealPlanService.getMealPlansByProfile(id));
    }

    @GetMapping("/meal-plan/current")
    public ResponseEntity<?> getCurrentMealPlan(
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String uid = resolveUserId(userId, headerUserId, null);
        var plan = mealPlanService.getCurrentMealPlan(uid);
        if (plan.isPresent()) {
            log.info("GET /api/nutrition/meal-plan/current found persisted active plan id={}",
                plan.get().getId());
        } else {
            log.info("GET /api/nutrition/meal-plan/current found no persisted active plan");
        }
        return plan
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/meal-plan/history")
    public ResponseEntity<List<MealPlanResponseDTO.HistoryItemDTO>> getHistory(
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String uid = resolveUserId(userId, headerUserId, null);
        return ResponseEntity.ok(mealPlanService.getHistory(uid));
    }

    @GetMapping("/meal-plan/{id}")
    public ResponseEntity<?> getMealPlanByIdLegacy(@PathVariable Long id) {
        return getMealPlanById(id);
    }

    @PostMapping("/meal-plan/{id}/activate")
    public ResponseEntity<?> activatePlan(
            @PathVariable Long id,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String uid = resolveUserId(userId, headerUserId, null);
        boolean ok = mealPlanService.activatePlan(id, uid);
        if (!ok) {
            return ResponseEntity.status(404).body(Map.of("error", "Programme non trouvé."));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Programme activé avec succès."));
    }

    @DeleteMapping("/meal-plan/{id}")
    public ResponseEntity<?> deletePlan(
            @PathVariable Long id,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String uid = resolveUserId(userId, headerUserId, null);
        boolean ok = mealPlanService.deletePlan(id, uid);
        if (!ok) {
            return ResponseEntity.status(404).body(Map.of("error", "Programme non trouvé."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/meal/{mealId}/toggle-complete")
    public ResponseEntity<?> toggleMealCompleted(
            @PathVariable Long mealId,
            @RequestBody Map<String, Object> body) {
        boolean isCompleted = Boolean.TRUE.equals(body.get("isCompleted"))
            || "true".equalsIgnoreCase(String.valueOf(body.get("isCompleted")));
        return mealPlanService.toggleMealCompleted(mealId, isCompleted)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/meal/{mealId}/regenerate")
    public ResponseEntity<?> regenerateMeal(
            @PathVariable Long mealId,
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String uid = resolveUserId(userId, headerUserId, null);
        return mealPlanService.regenerateMeal(mealId, uid)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(404).body(Map.of("error", "Repas introuvable.")));
    }

    @PostMapping("/meal/{mealId}/replace-food")
    public ResponseEntity<?> replaceFood(
            @PathVariable Long mealId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long foodId = null;
        if (body != null && body.get("foodId") != null) {
            try {
                foodId = Long.parseLong(body.get("foodId").toString());
            } catch (NumberFormatException ignored) {
                foodId = null;
            }
        }
        try {
            return mealPlanService.replaceFood(mealId, foodId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Repas introuvable.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    private String resolveUserId(String queryUserId, String headerUserId, String bodyUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) return headerUserId.trim();
        if (queryUserId != null && !queryUserId.isBlank()) return queryUserId.trim();
        if (bodyUserId != null && !bodyUserId.isBlank()) return bodyUserId.trim();
        return "default_user_1";
    }
}
