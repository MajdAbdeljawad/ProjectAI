package com.example.backend_meal.nutrition;

import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.service.MealPlanService;
import com.example.backend_meal.nutrition.service.NutritionProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NutritionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutritionProfileService profileService;

    @Autowired
    private MealPlanService mealPlanService;

    @Test
    void profileEndpointReturnsPersistedProfile() throws Exception {
        NutritionProfile profile = profileService.createOrUpdate(buildProfile("profile-route-user"));

        mockMvc.perform(get("/api/nutrition/profile")
                .param("userId", "profile-route-user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(profile.getId().toString()))
            .andExpect(jsonPath("$.userId").value("profile-route-user"));
    }

    @Test
    void currentEndpointReturnsPersistedActivePlan() throws Exception {
        profileService.createOrUpdate(buildProfile("current-route-user"));
        var savedPlan = mealPlanService.generateAndSave(new NutritionProfile(), "current-route-user");

        mockMvc.perform(get("/api/nutrition/meal-plan/current")
                .param("userId", "current-route-user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(savedPlan.getId()))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.generatedBy.isRealAI").value(false));
    }

    @Test
    void regenerateEndpointUsesPersistedProfileAndSavesPlan() throws Exception {
        profileService.createOrUpdate(buildProfile("regenerate-route-user"));

        mockMvc.perform(post("/api/nutrition/meal-plan/regenerate")
                .param("userId", "regenerate-route-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.generatedBy.usedFallback").value(true));
    }

    private NutritionProfile buildProfile(String userId) {
        NutritionProfile profile = new NutritionProfile();
        profile.setUserId(userId);
        profile.setFullName("Route test");
        profile.setAge(25);
        profile.setSex("homme");
        profile.setHeight(175.0);
        profile.setWeight(75.0);
        profile.setGoal("maintien");
        profile.setSportsLevel("intermediaire");
        profile.setSessionsPerWeek(3);
        profile.setWorkoutType("running");
        profile.setDiet("omnivore");
        profile.setMealsPerDay(3);
        return profile;
    }
}
