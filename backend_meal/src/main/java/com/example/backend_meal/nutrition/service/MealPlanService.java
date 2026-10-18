package com.example.backend_meal.nutrition.service;

import com.example.backend_meal.ai.AIService;
import com.example.backend_meal.nutrition.dto.MealPlanResponseDTO;
import com.example.backend_meal.nutrition.exception.ValidationException;
import com.example.backend_meal.nutrition.model.*;
import com.example.backend_meal.nutrition.repository.MealPlanRepository;
import com.example.backend_meal.nutrition.repository.MealRepository;
import com.example.backend_meal.nutrition.repository.NutritionProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates meal plan generation, persistence and Angular-facing CRUD.
 */
@Service
public class MealPlanService {

    private final AIService aiService;
    private final NutritionCalculatorService calculator;
    private final NutritionProfileService profileService;
    private final MealPlanValidator validator;
    private final ObjectMapper objectMapper;

    private final NutritionProfileRepository profileRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealRepository mealRepository;

    public MealPlanService(AIService aiService,
                           NutritionCalculatorService calculator,
                           NutritionProfileService profileService,
                           MealPlanValidator validator,
                           NutritionProfileRepository profileRepository,
                           MealPlanRepository mealPlanRepository,
                           MealRepository mealRepository) {
        this.aiService = aiService;
        this.calculator = calculator;
        this.profileService = profileService;
        this.validator = validator;
        this.profileRepository = profileRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.mealRepository = mealRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public MealPlanResponseDTO generateAndSave(NutritionProfile profileInput, String userId) throws Exception {
        NutritionProfile profile = resolveProfile(profileInput, userId);

        System.out.printf("[MealPlanService] Génération pour profil id=%d (%s) | Objectif: %s | %d kcal | Fournisseur: %s | IA réelle: %b%n",
            profile.getId(), profile.getFullName(), profile.getGoal(),
            profile.getTargetCalories(), aiService.getProviderName(), aiService.isRealAI());

        String rawJson = aiService.generatePlan(profile);

        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(rawJson, Map.class);
        } catch (Exception e) {
            throw new ValidationException("La réponse IA n'est pas du JSON valide: " + e.getMessage());
        }

        validator.validate(parsed, profile);
        validator.validateAndRecalculate(parsed);

        mealPlanRepository.findByNutritionProfileAndIsActiveTrue(profile).ifPresent(prev -> {
            prev.setIsActive(false);
            mealPlanRepository.save(prev);
        });

        Map<String, Object> mealPlanMap = (Map<String, Object>) parsed.get("mealPlan");
        List<Map<String, Object>> daysRaw = (List<Map<String, Object>>) mealPlanMap.get("days");

        String goalLabel = profile.getGoal() != null ? profile.getGoal().replace('_', ' ') : "personnalisé";
        String title = "Programme personnalisé - " + goalLabel.toUpperCase();

        MealPlan mealPlan = MealPlan.builder()
            .nutritionProfile(profile)
            .title(title)
            .durationDays(7)
            .caloriesTarget(profile.getTargetCalories())
            .proteinTarget(profile.getTargetProtein())
            .carbsTarget(profile.getTargetCarbs())
            .fatsTarget(profile.getTargetFats())
            .generatedByAI(aiService.isRealAI())
            .providerName(aiService.getProviderName())
            .model(aiService.getModel())
            .isActive(true)
            .days(new ArrayList<>())
            .build();

        for (int d = 0; d < daysRaw.size(); d++) {
            Map<String, Object> dayRaw = daysRaw.get(d);
            List<Map<String, Object>> mealsRaw = (List<Map<String, Object>>) dayRaw.get("meals");

            DailyPlan dailyPlan = DailyPlan.builder()
                .mealPlan(mealPlan)
                .dayNumber(d + 1)
                .dayName(str(dayRaw.get("day")))
                .totalCalories(toInt(dayRaw.get("totalCalories")))
                .totalProtein(toInt(dayRaw.get("totalProtein")))
                .totalCarbs(toInt(dayRaw.get("totalCarbs")))
                .totalFats(toInt(dayRaw.get("totalFats")))
                .meals(new ArrayList<>())
                .build();

            for (int m = 0; m < mealsRaw.size(); m++) {
                Map<String, Object> mealRaw = mealsRaw.get(m);
                List<Map<String, Object>> foodsRaw = (List<Map<String, Object>>) mealRaw.get("foods");
                List<?> altsRaw = (List<?>) mealRaw.get("alternatives");

                Meal meal = Meal.builder()
                    .dailyPlan(dailyPlan)
                    .type(str(mealRaw.get("type")))
                    .name(str(mealRaw.get("name")))
                    .calories(toInt(mealRaw.get("calories")))
                    .protein(toInt(mealRaw.get("protein")))
                    .carbs(toInt(mealRaw.get("carbs")))
                    .fats(toInt(mealRaw.get("fats")))
                    .preparationTime(str(mealRaw.get("preparationTime")))
                    .preparation(str(mealRaw.get("preparation")))
                    .orderIndex(m)
                    .isCompleted(false)
                    .foods(new ArrayList<>())
                    .alternatives(new ArrayList<>())
                    .build();

                if (foodsRaw != null) {
                    for (Map<String, Object> foodRaw : foodsRaw) {
                        Food food = Food.builder()
                            .meal(meal)
                            .name(str(foodRaw.get("name")))
                            .quantity(str(foodRaw.get("quantity")))
                            .calories(toInt(foodRaw.get("calories")))
                            .protein(toInt(foodRaw.get("protein")))
                            .carbs(toInt(foodRaw.get("carbs")))
                            .fats(toInt(foodRaw.get("fats")))
                            .build();
                        meal.getFoods().add(food);
                    }
                }

                if (altsRaw != null) {
                    for (Object alt : altsRaw) {
                        if (alt != null && !alt.toString().isBlank()) {
                            meal.getAlternatives().add(MealAlternative.builder()
                                .meal(meal)
                                .name(alt.toString())
                                .build());
                        }
                    }
                }

                dailyPlan.getMeals().add(meal);
            }
            mealPlan.getDays().add(dailyPlan);
        }

        MealPlan saved = mealPlanRepository.save(mealPlan);
        initializeGraph(saved);

        System.out.printf("[MealPlanService] ✅ MealPlan id=%d sauvegardé | %d jours | IA réelle: %b%n",
            saved.getId(), saved.getDays().size(), saved.getGeneratedByAI());

        return MealPlanResponseDTO.from(saved, aiService.isRealAI(), !aiService.isRealAI());
    }

    @Transactional(readOnly = true)
    public List<MealPlanResponseDTO> getAllMealPlans() {
        List<MealPlan> plans = mealPlanRepository.findAllByOrderByCreatedAtDesc();
        plans.forEach(this::initializeGraph);
        return plans.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<MealPlanResponseDTO> getMealPlanById(Long id) {
        return mealPlanRepository.findById(id)
            .map(p -> {
                initializeGraph(p);
                return toDto(p);
            });
    }

    @Transactional(readOnly = true)
    public List<MealPlanResponseDTO> getMealPlansByProfile(Long profileId) {
        return profileRepository.findById(profileId)
            .map(profile -> {
                List<MealPlan> plans = mealPlanRepository.findByNutritionProfileOrderByCreatedAtDesc(profile);
                plans.forEach(this::initializeGraph);
                return plans.stream().map(this::toDto).collect(Collectors.toList());
            })
            .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Optional<MealPlanResponseDTO> getCurrentMealPlan(String userId) {
        return resolveProfileByUserId(userId)
            .flatMap(mealPlanRepository::findFirstByNutritionProfileAndIsActiveTrueOrderByCreatedAtDesc)
            .map(p -> {
                initializeGraph(p);
                return toDto(p);
            });
    }

    @Transactional(readOnly = true)
    public List<MealPlanResponseDTO.HistoryItemDTO> getHistory(String userId) {
        return resolveProfileByUserId(userId)
            .map(profile -> mealPlanRepository.findByNutritionProfileOrderByCreatedAtDesc(profile).stream()
                .map(MealPlanResponseDTO::toHistoryItem)
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    @Transactional
    public boolean activatePlan(Long planId, String userId) {
        Optional<MealPlan> opt = mealPlanRepository.findById(planId);
        if (opt.isEmpty()) return false;
        MealPlan plan = opt.get();
        NutritionProfile profile = plan.getNutritionProfile();
        if (userId != null && profile.getUserId() != null && !userId.equals(profile.getUserId())) {
            return false;
        }
        mealPlanRepository.findByNutritionProfileAndIsActiveTrue(profile).ifPresent(prev -> {
            if (!prev.getId().equals(planId)) {
                prev.setIsActive(false);
                mealPlanRepository.save(prev);
            }
        });
        plan.setIsActive(true);
        mealPlanRepository.save(plan);
        return true;
    }

    @Transactional
    public boolean deletePlan(Long planId, String userId) {
        Optional<MealPlan> opt = mealPlanRepository.findById(planId);
        if (opt.isEmpty()) return false;
        MealPlan plan = opt.get();
        NutritionProfile profile = plan.getNutritionProfile();
        if (userId != null && profile.getUserId() != null && !userId.equals(profile.getUserId())) {
            return false;
        }
        mealPlanRepository.delete(plan);
        return true;
    }

    @Transactional
    public Optional<MealPlanResponseDTO.MealDTO> toggleMealCompleted(Long mealId, boolean isCompleted) {
        return mealRepository.findById(mealId).map(meal -> {
            meal.setIsCompleted(isCompleted);
            Meal saved = mealRepository.save(meal);
            initializeMeal(saved);
            return MealPlanResponseDTO.fromMeal(saved);
        });
    }

    @Transactional
    public Optional<MealPlanResponseDTO.MealDTO> regenerateMeal(Long mealId, String userId) {
        return mealRepository.findById(mealId).map(meal -> {
            initializeMeal(meal);
            NutritionProfile profile = meal.getDailyPlan().getMealPlan().getNutritionProfile();
            // Algorithmic swap: keep macros, rotate protein/carb names (works without OpenAI).
            if (!meal.getFoods().isEmpty()) {
                Food first = meal.getFoods().get(0);
                first.setName("Variante — " + first.getName().replace("Variante — ", ""));
            }
            meal.setName("Régénéré : " + meal.getName().replace("Régénéré : ", ""));
            meal.setIsCompleted(false);
            Meal saved = mealRepository.save(meal);
            initializeMeal(saved);
            return MealPlanResponseDTO.fromMeal(saved);
        });
    }

    @Transactional
    public Optional<MealPlanResponseDTO.MealDTO> replaceFood(Long mealId, Long foodId) {
        return mealRepository.findById(mealId).map(meal -> {
            initializeMeal(meal);
            Food target = meal.getFoods().stream()
                .filter(f -> foodId == null || f.getId().equals(foodId))
                .findFirst()
                .orElse(meal.getFoods().isEmpty() ? null : meal.getFoods().get(0));
            if (target == null) {
                throw new IllegalArgumentException("Aliment introuvable dans ce repas.");
            }
            boolean isProtein = target.getProtein() != null && target.getCarbs() != null
                && target.getProtein() > target.getCarbs();
            target.setName(isProtein
                ? "Pavé de truite grillé aux herbes"
                : "Patates douces rôties au four");
            recalculateMealFromFoods(meal);
            Meal saved = mealRepository.save(meal);
            initializeMeal(saved);
            return MealPlanResponseDTO.fromMeal(saved);
        });
    }

    private NutritionProfile resolveProfile(NutritionProfile profileInput, String userId) {
        String uid = firstNonBlank(
            profileInput != null ? profileInput.getUserId() : null,
            userId,
            "default_user_1"
        );

        if (!isProfileComplete(profileInput)) {
            return profileService.findByUserId(uid)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Impossible de générer un programme sans profil. Veuillez d'abord enregistrer votre profil nutritionnel."));
        }

        profileInput.setUserId(uid);
        calculator.calculate(profileInput);
        return profileService.createOrUpdate(profileInput);
    }

    private Optional<NutritionProfile> resolveProfileByUserId(String userId) {
        String uid = firstNonBlank(userId, "default_user_1");
        return profileService.findByUserId(uid);
    }

    private boolean isProfileComplete(NutritionProfile p) {
        return p != null && p.getAge() != null && p.getHeight() != null && p.getWeight() != null;
    }

    private MealPlanResponseDTO toDto(MealPlan p) {
        boolean ai = Boolean.TRUE.equals(p.getGeneratedByAI());
        return MealPlanResponseDTO.from(p, ai, !ai);
    }

    private void initializeGraph(MealPlan plan) {
        if (plan.getNutritionProfile() != null) {
            plan.getNutritionProfile().getUserId();
        }
        plan.getDays().forEach(day ->
            day.getMeals().forEach(this::initializeMeal));
    }

    private void initializeMeal(Meal meal) {
        meal.getFoods().size();
        meal.getAlternatives().size();
    }

    private void recalculateMealFromFoods(Meal meal) {
        int cal = 0, prot = 0, carbs = 0, fats = 0;
        for (Food f : meal.getFoods()) {
            cal += f.getCalories() != null ? f.getCalories() : 0;
            prot += f.getProtein() != null ? f.getProtein() : 0;
            carbs += f.getCarbs() != null ? f.getCarbs() : 0;
            fats += f.getFats() != null ? f.getFats() : 0;
        }
        meal.setCalories(cal);
        meal.setProtein(prot);
        meal.setCarbs(carbs);
        meal.setFats(fats);
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
