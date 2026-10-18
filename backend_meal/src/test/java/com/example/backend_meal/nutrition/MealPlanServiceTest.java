package com.example.backend_meal.nutrition;

import com.example.backend_meal.nutrition.dto.MealPlanResponseDTO;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.service.MealPlanService;
import com.example.backend_meal.nutrition.service.NutritionProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MealPlanServiceTest {

    @Autowired
    MealPlanService mealPlanService;

    @Autowired
    NutritionProfileService profileService;

    @Test
    @DisplayName("Generate without saved profile throws")
    void generate_withoutProfile_throws() {
        assertThatThrownBy(() -> mealPlanService.generateAndSave(new NutritionProfile(), "missing_user"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("profil");
    }

    @Test
    @DisplayName("Generate + persist 7-day plan for saved profile")
    void generateAndSave_persistsSevenDays() throws Exception {
        NutritionProfile profile = buildProfile("user_plan_1", "Hugo");
        profileService.createOrUpdate(profile);

        MealPlanResponseDTO dto = mealPlanService.generateAndSave(new NutritionProfile(), "user_plan_1");

        assertThat(dto.getId()).isNotBlank();
        assertThat(dto.getDays()).hasSize(7);
        assertThat(dto.getIsActive()).isTrue();
        assertThat(dto.getDailyTarget().getCalories()).isGreaterThan(0);
        assertThat(dto.getDays().get(0).getMeals()).isNotEmpty();
        assertThat(dto.getDays().get(0).getMeals().get(0).getFoods()).isNotEmpty();
        assertThat(dto.getGeneratedBy()).containsEntry("usedFallback", true);
    }

    @Test
    @DisplayName("Current plan is the active one; history lists both after second generate")
    void currentAndHistory_afterSecondGenerate() throws Exception {
        profileService.createOrUpdate(buildProfile("user_plan_2", "Ines"));

        MealPlanResponseDTO first = mealPlanService.generateAndSave(new NutritionProfile(), "user_plan_2");
        MealPlanResponseDTO second = mealPlanService.generateAndSave(new NutritionProfile(), "user_plan_2");

        MealPlanResponseDTO current = mealPlanService.getCurrentMealPlan("user_plan_2").orElseThrow();
        assertThat(current.getId()).isEqualTo(second.getId());
        assertThat(current.getIsActive()).isTrue();

        var history = mealPlanService.getHistory("user_plan_2");
        assertThat(history).hasSize(2);
        assertThat(history.stream().filter(h -> Boolean.TRUE.equals(h.getIsActive()))).hasSize(1);

        assertThat(mealPlanService.activatePlan(Long.parseLong(first.getId()), "user_plan_2")).isTrue();
        MealPlanResponseDTO reactivated = mealPlanService.getCurrentMealPlan("user_plan_2").orElseThrow();
        assertThat(reactivated.getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("Toggle meal completed persists")
    void toggleMealCompleted_persists() throws Exception {
        profileService.createOrUpdate(buildProfile("user_plan_3", "Jules"));
        MealPlanResponseDTO plan = mealPlanService.generateAndSave(new NutritionProfile(), "user_plan_3");
        String mealId = plan.getDays().get(0).getMeals().get(0).getId();

        MealPlanResponseDTO.MealDTO updated = mealPlanService
            .toggleMealCompleted(Long.parseLong(mealId), true)
            .orElseThrow();

        assertThat(updated.getIsCompleted()).isTrue();
    }

    private NutritionProfile buildProfile(String userId, String fullName) {
        NutritionProfile p = new NutritionProfile();
        p.setUserId(userId);
        p.setFullName(fullName);
        p.setAge(25);
        p.setSex("homme");
        p.setHeight(175.0);
        p.setWeight(75.0);
        p.setGoal("maintien");
        p.setSportsLevel("intermediaire");
        p.setSessionsPerWeek(3);
        p.setWorkoutType("running");
        p.setDiet("omnivore");
        p.setMealsPerDay(3);
        return p;
    }
}
