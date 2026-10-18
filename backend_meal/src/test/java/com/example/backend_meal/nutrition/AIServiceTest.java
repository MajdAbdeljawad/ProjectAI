package com.example.backend_meal.nutrition;

import com.example.backend_meal.ai.AIService;
import com.example.backend_meal.nutrition.exception.ValidationException;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.service.MealPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AIService — verifies provider selection, isRealAI flag,
 * and that buildPrompt() includes allergens and calorie targets.
 * No real OpenAI call is made (test profile uses dynamic provider).
 */
@SpringBootTest
@ActiveProfiles("test")
class AIServiceTest {

    @Autowired
    AIService aiService;

    @Autowired
    MealPlanValidator validator;

    private NutritionProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new NutritionProfile();
        testProfile.setFullName("Test User");
        testProfile.setAge(25);
        testProfile.setSex("homme");
        testProfile.setHeight(175.0);
        testProfile.setWeight(75.0);
        testProfile.setGoal("maintien");
        testProfile.setSportsLevel("intermediaire");
        testProfile.setSessionsPerWeek(3);
        testProfile.setWorkoutType("running");
        testProfile.setDiet("omnivore");
        testProfile.setMealsPerDay(3);
        testProfile.setTargetCalories(2000);
        testProfile.setTargetProtein(150);
        testProfile.setTargetCarbs(200);
        testProfile.setTargetFats(67);
        testProfile.setAllergies(List.of("arachides", "gluten"));
    }

    @Test
    @DisplayName("In test profile (no API key) — isRealAI returns false")
    void isRealAI_falseWithoutKey() {
        // test profile uses ai.provider=dynamic, no key
        assertThat(aiService.isRealAI()).isFalse();
    }

    @Test
    @DisplayName("ProviderName is non-null and non-empty")
    void providerName_nonNull() {
        assertThat(aiService.getProviderName()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("buildPrompt() contains allergen list")
    void buildPrompt_containsAllergens() {
        String prompt = aiService.buildPrompt(testProfile);
        assertThat(prompt).contains("arachides");
        assertThat(prompt).contains("gluten");
    }

    @Test
    @DisplayName("buildPrompt() contains calorie target")
    void buildPrompt_containsCalorieTarget() {
        String prompt = aiService.buildPrompt(testProfile);
        assertThat(prompt).contains("2000");
    }

    @Test
    @DisplayName("buildPrompt() contains all 7 day names")
    void buildPrompt_contains7Days() {
        String prompt = aiService.buildPrompt(testProfile);
        assertThat(prompt).contains("Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche");
    }

    @Test
    @DisplayName("buildPrompt() contains explicit sum coherence instruction (somme)")
    void buildPrompt_containsSumCoherenceRule() {
        String prompt = aiService.buildPrompt(testProfile);
        // Prompt is in French — coherence rule uses "somme" (= sum)
        assertThat(prompt).containsIgnoringCase("somme");
    }

    @Test
    @DisplayName("generatePlan() with fallback (no key) returns valid JSON")
    void generatePlan_fallback_returnsValidJson() throws Exception {
        String json = aiService.generatePlan(testProfile);
        assertThat(json).isNotNull().isNotBlank().contains("mealPlan");
    }

    @Test
    @DisplayName("Fallback JSON has exactly 7 days")
    void fallbackJson_has7Days() throws Exception {
        String json = aiService.generatePlan(testProfile);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> parsed = mapper.readValue(json, java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> mp = (java.util.Map<String, Object>) parsed.get("mealPlan");
        @SuppressWarnings("unchecked")
        java.util.List<?> days = (java.util.List<?>) mp.get("days");
        assertThat(days).hasSize(7);
    }

    @Test
    @DisplayName("Fallback JSON meal calories equal the sum of foods")
    void fallbackJson_mealCaloriesMatchFoods() throws Exception {
        String json = aiService.generatePlan(testProfile);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> parsed = mapper.readValue(json, java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> mp = (java.util.Map<String, Object>) parsed.get("mealPlan");
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> days =
            (java.util.List<java.util.Map<String, Object>>) mp.get("days");
        for (java.util.Map<String, Object> day : days) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> meals =
                (java.util.List<java.util.Map<String, Object>>) day.get("meals");
            for (java.util.Map<String, Object> meal : meals) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> foods =
                    (java.util.List<java.util.Map<String, Object>>) meal.get("foods");
                int foodSum = foods.stream().mapToInt(f -> ((Number) f.get("calories")).intValue()).sum();
                assertThat(((Number) meal.get("calories")).intValue()).isEqualTo(foodSum);
            }
        }
    }

    @Test
    @DisplayName("getModel() returns non-null value")
    void getModel_nonNull() {
        assertThat(aiService.getModel()).isNotNull();
    }
}
