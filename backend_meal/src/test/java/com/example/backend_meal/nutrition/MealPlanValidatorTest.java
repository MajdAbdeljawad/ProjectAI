package com.example.backend_meal.nutrition;

import com.example.backend_meal.nutrition.exception.ValidationException;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.service.MealPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MealPlanValidator — no Spring context needed.
 */
class MealPlanValidatorTest {

    private MealPlanValidator validator;
    private NutritionProfile noAllergyProfile;

    @BeforeEach
    void setUp() {
        validator = new MealPlanValidator();

        noAllergyProfile = new NutritionProfile();
        noAllergyProfile.setAllergies(new ArrayList<>());
        noAllergyProfile.setDiet("omnivore");
    }

    // ── Structure tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid 7-day plan passes structure validation")
    void validPlan_passes() {
        Map<String, Object> parsed = buildValidParsed(7, 3);
        assertThatCode(() -> validator.validate(parsed, noAllergyProfile))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Plan with 6 days fails validation")
    void sixDayPlan_fails() {
        Map<String, Object> parsed = buildValidParsed(6, 3);
        assertThatThrownBy(() -> validator.validate(parsed, noAllergyProfile))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("7 jours");
    }

    @Test
    @DisplayName("Missing mealPlan key fails validation")
    void missingMealPlanKey_fails() {
        assertThatThrownBy(() -> validator.validate(new HashMap<>(), noAllergyProfile))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("mealPlan");
    }

    @Test
    @DisplayName("Day with empty meals list fails validation")
    void emptyMealsInDay_fails() {
        Map<String, Object> parsed = buildValidParsed(7, 0); // 0 meals per day
        assertThatThrownBy(() -> validator.validate(parsed, noAllergyProfile))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("meals");
    }

    // ── Recalculation tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Meal calorie total is recalculated from foods (source of truth)")
    void mealTotals_recalculatedFromFoods() {
        Map<String, Object> parsed = buildValidParsed(7, 1);

        // Inject wrong GPT total for day 0, meal 0
        @SuppressWarnings("unchecked")
        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("mealPlan");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> days = (List<Map<String, Object>>) mealPlan.get("days");
        @SuppressWarnings("unchecked")
        Map<String, Object> day0 = days.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meals = (List<Map<String, Object>>) day0.get("meals");
        meals.get(0).put("calories", 9999); // wrong GPT value

        // After recalculate, value should be sum of food calories (200+300=500 per buildValidFood)
        validator.validateAndRecalculate(parsed);

        int recalculated = (int) meals.get(0).get("calories");
        assertThat(recalculated).isNotEqualTo(9999);
        assertThat(recalculated).isEqualTo(500); // 2 foods × 250 each from buildValidFood
    }

    // ── Allergen tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("No allergen violations on clean plan")
    void noViolations_cleanPlan() {
        Map<String, Object> parsed = buildValidParsed(7, 2);
        NutritionProfile profile = new NutritionProfile();
        profile.setAllergies(List.of("arachides"));
        profile.setDiet("omnivore");

        List<String> violations = validator.findAllergenViolations(parsed, profile);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Peanuts allergen detected in food name")
    void peanutAllergen_detected() {
        Map<String, Object> parsed = buildParsedWithFood("Beurre de cacahuète", 7, 1);
        NutritionProfile profile = new NutritionProfile();
        profile.setAllergies(List.of("arachides"));
        profile.setDiet("omnivore");

        List<String> violations = validator.findAllergenViolations(parsed, profile);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("'peanuts' as allergen also detects 'arachides' (synonym)")
    void peanuts_synonymDetected() {
        Map<String, Object> parsed = buildParsedWithFood("Salade aux arachides grillées", 7, 1);
        NutritionProfile profile = new NutritionProfile();
        profile.setAllergies(List.of("peanuts")); // declared as "peanuts"
        profile.setDiet("omnivore");

        List<String> violations = validator.findAllergenViolations(parsed, profile);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validate throws when allergen violation detected")
    void validate_throwsOnAllergen() {
        Map<String, Object> parsed = buildParsedWithFood("Cacahuètes grillées", 7, 1);
        NutritionProfile profile = new NutritionProfile();
        profile.setAllergies(List.of("arachides"));
        profile.setDiet("omnivore");

        assertThatThrownBy(() -> validator.validate(parsed, profile))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("allerg");
    }

    @Test
    @DisplayName("Null allergies profile — no violation")
    void nullAllergies_noViolation() {
        Map<String, Object> parsed = buildValidParsed(7, 2);
        NutritionProfile profile = new NutritionProfile();
        profile.setAllergies(null);
        profile.setDiet("omnivore");

        assertThatCode(() -> validator.validate(parsed, profile))
            .doesNotThrowAnyException();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildValidParsed(int numDays, int mealsPerDay) {
        List<Map<String, Object>> days = new ArrayList<>();
        String[] dayNames = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};
        for (int d = 0; d < numDays; d++) {
            List<Map<String, Object>> meals = new ArrayList<>();
            for (int m = 0; m < Math.max(mealsPerDay, 1); m++) {
                meals.add(buildValidMeal("Repas " + m, m));
            }
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", dayNames[d % dayNames.length]);
            day.put("meals", mealsPerDay == 0 ? new ArrayList<>() : meals);
            days.add(day);
        }
        Map<String, Object> mealPlan = new LinkedHashMap<>();
        mealPlan.put("durationDays", numDays);
        mealPlan.put("dailyTarget", Map.of("calories",2000,"protein",150,"carbs",200,"fats",67));
        mealPlan.put("days", days);
        return Map.of("mealPlan", mealPlan);
    }

    private Map<String, Object> buildValidMeal(String name, int index) {
        List<Map<String, Object>> foods = List.of(
            buildValidFood("Aliment A", 250),
            buildValidFood("Aliment B", 250)
        );
        Map<String, Object> meal = new LinkedHashMap<>();
        meal.put("type", "breakfast");
        meal.put("name", name);
        meal.put("calories", 500);
        meal.put("protein", 40);
        meal.put("carbs", 50);
        meal.put("fats", 15);
        meal.put("preparationTime", "15 min");
        meal.put("preparation", "Instructions...");
        meal.put("alternatives", List.of("Alt 1", "Alt 2"));
        meal.put("foods", new ArrayList<>(foods));
        return meal;
    }

    private Map<String, Object> buildValidFood(String name, int calories) {
        return Map.of("name", name, "quantity", "100g",
            "calories", calories, "protein", 20, "carbs", 25, "fats", 8);
    }

    private Map<String, Object> buildParsedWithFood(String foodName, int numDays, int mealsPerDay) {
        Map<String, Object> parsed = buildValidParsed(numDays, mealsPerDay);
        // Inject the specific food name into the first meal of day 0
        @SuppressWarnings("unchecked")
        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("mealPlan");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> days = (List<Map<String, Object>>) mealPlan.get("days");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> meals = (List<Map<String, Object>>) days.get(0).get("meals");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> foods = (List<Map<String, Object>>) meals.get(0).get("foods");
        foods.add(new LinkedHashMap<>(Map.of(
            "name", foodName, "quantity", "50g",
            "calories", 150, "protein", 5, "carbs", 3, "fats", 12
        )));
        return parsed;
    }
}
