package com.example.backend_meal.nutrition;

import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.repository.NutritionProfileRepository;
import com.example.backend_meal.nutrition.service.NutritionCalculatorService;
import com.example.backend_meal.nutrition.service.NutritionProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for NutritionProfileService using H2 in-memory database.
 * No real MySQL, no real OpenAI key needed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NutritionProfileServiceTest {

    @Autowired
    NutritionProfileService profileService;

    @Autowired
    NutritionProfileRepository profileRepository;

    @Test
    @DisplayName("Create profile — persisted in DB with calculated targets")
    void createProfile_persisted() {
        NutritionProfile profile = buildProfile("user_test_1", "Alice Dupont");
        NutritionProfile saved = profileService.createOrUpdate(profile);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFullName()).isEqualTo("Alice Dupont");
        assertThat(saved.getTargetCalories()).isGreaterThan(0);
        assertThat(saved.getBmr()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Same userId — returns updated profile, not duplicate")
    void sameUserId_updatesExisting() {
        NutritionProfile p1 = buildProfile("user_test_2", "Bob Martin");
        profileService.createOrUpdate(p1);

        NutritionProfile p2 = buildProfile("user_test_2", "Bob Martin Updated");
        p2.setWeight(90.0);
        NutritionProfile saved = profileService.createOrUpdate(p2);

        long count = profileRepository.findAll().stream()
            .filter(p -> "user_test_2".equals(p.getUserId()))
            .count();
        assertThat(count).isEqualTo(1);
        assertThat(saved.getFullName()).isEqualTo("Bob Martin Updated");
    }

    @Test
    @DisplayName("Update profile by id — weight change recalculates macros")
    void updateProfile_weightChange_recalculates() {
        NutritionProfile original = buildProfile("user_test_3", "Charlie");
        original.setWeight(70.0);
        NutritionProfile saved = profileService.createOrUpdate(original);
        int originalCalories = saved.getTargetCalories();

        NutritionProfile update = new NutritionProfile();
        update.setWeight(95.0); // heavier → more calories
        NutritionProfile updated = profileService.update(saved.getId(), update);

        assertThat(updated.getWeight()).isEqualTo(95.0);
        assertThat(updated.getTargetCalories()).isGreaterThan(originalCalories);
    }

    @Test
    @DisplayName("FindById returns correct profile")
    void findById_returnsProfile() {
        NutritionProfile saved = profileService.createOrUpdate(buildProfile("user_test_4", "Diana"));
        assertThat(profileService.findById(saved.getId())).isPresent();
        assertThat(profileService.findById(saved.getId()).get().getFullName()).isEqualTo("Diana");
    }

    @Test
    @DisplayName("FindByUserId returns correct profile")
    void findByUserId_returnsProfile() {
        profileService.createOrUpdate(buildProfile("user_unique_99", "Eve"));
        assertThat(profileService.findByUserId("user_unique_99")).isPresent();
    }

    @Test
    @DisplayName("Allergies are persisted and retrieved")
    void allergies_persisted() {
        NutritionProfile profile = buildProfile("user_test_5", "Frank");
        profile.setAllergies(List.of("arachides", "gluten"));
        NutritionProfile saved = profileService.createOrUpdate(profile);

        NutritionProfile found = profileService.findById(saved.getId()).orElseThrow();
        assertThat(found.getAllergies()).containsExactlyInAnyOrder("arachides", "gluten");
    }

    @Test
    @DisplayName("Goal change updates target calories correctly")
    void goalChange_updatesCalories() {
        // Use a heavier profile so prise_de_masse target > maintien target (no rounding collision)
        NutritionProfile profile = buildProfile("user_test_6", "Grace");
        profile.setGoal("maintien");
        profile.setWeight(90.0);   // 90kg → noticeable calorie difference between goals
        profile.setHeight(180.0);
        profile.setAge(28);
        NutritionProfile maintien = profileService.createOrUpdate(profile);
        // Capture value before update — JPA entity aliasing in same transaction
        int maintienCalories = maintien.getTargetCalories();

        NutritionProfile update = new NutritionProfile();
        update.setGoal("prise_de_masse");
        NutritionProfile masse = profileService.update(maintien.getId(), update);

        // Muscle gain adds +350 kcal to TDEE → must be strictly greater than maintien
        assertThat(masse.getTargetCalories()).isGreaterThan(maintienCalories);
    }

    // ── Helper ──────────────────────────────────────────────────────────────

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
