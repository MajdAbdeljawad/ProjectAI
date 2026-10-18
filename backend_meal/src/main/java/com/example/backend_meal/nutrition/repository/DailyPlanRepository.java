package com.example.backend_meal.nutrition.repository;

import com.example.backend_meal.nutrition.model.DailyPlan;
import com.example.backend_meal.nutrition.model.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    List<DailyPlan> findByMealPlanOrderByDayNumberAsc(MealPlan mealPlan);
}
