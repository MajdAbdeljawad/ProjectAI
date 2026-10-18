package com.example.backend_meal.nutrition.repository;

import com.example.backend_meal.nutrition.model.MealPlan;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    List<MealPlan> findByNutritionProfileOrderByCreatedAtDesc(NutritionProfile profile);
    Optional<MealPlan> findByNutritionProfileAndIsActiveTrue(NutritionProfile profile);
    Optional<MealPlan> findFirstByNutritionProfileAndIsActiveTrueOrderByCreatedAtDesc(NutritionProfile profile);
    List<MealPlan> findAllByOrderByCreatedAtDesc();
    Optional<MealPlan> findFirstByIsActiveTrueOrderByCreatedAtDesc();
}
