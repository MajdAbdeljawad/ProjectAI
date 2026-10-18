package com.example.backend_meal.nutrition.dto;

import com.example.backend_meal.nutrition.model.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for MealPlan response — maps entity graph to the JSON structure expected by Angular.
 * Entity IDs (Long) are serialized as strings to match TypeScript {@code id: string}.
 */
@Data
@Builder
public class MealPlanResponseDTO {

    private String id;
    private String userId;
    private String title;
    private Integer durationDays;
    private MacroTarget dailyTarget;
    private List<DailyPlanDTO> days;
    private Boolean generatedByAI;
    private String providerName;
    private String model;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Map<String, Object> generatedBy;

    @Data
    @Builder
    public static class MacroTarget {
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fats;
    }

    @Data
    @Builder
    public static class DailyPlanDTO {
        private String id;
        private String day;
        private Integer dayNumber;
        private Integer totalCalories;
        private Integer totalProtein;
        private Integer totalCarbs;
        private Integer totalFats;
        private List<MealDTO> meals;
    }

    @Data
    @Builder
    public static class MealDTO {
        private String id;
        private String type;
        private String name;
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fats;
        private String preparationTime;
        private String preparation;
        private List<String> alternatives;
        private List<FoodDTO> foods;
        private Integer orderIndex;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    public static class FoodDTO {
        private String id;
        private String name;
        private String quantity;
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fats;
    }

    @Data
    @Builder
    public static class HistoryItemDTO {
        private String id;
        private String title;
        private LocalDateTime createdAt;
        private Boolean isActive;
        private Integer targetCalories;
        private Integer durationDays;
    }

    public static MealPlanResponseDTO from(MealPlan plan, boolean isRealAI, boolean usedFallback) {
        List<DailyPlanDTO> dayDTOs = plan.getDays().stream()
            .map(MealPlanResponseDTO::fromDailyPlan)
            .collect(Collectors.toList());

        String userId = plan.getNutritionProfile() != null ? plan.getNutritionProfile().getUserId() : null;

        return MealPlanResponseDTO.builder()
            .id(idToString(plan.getId()))
            .userId(userId)
            .title(plan.getTitle())
            .durationDays(plan.getDurationDays())
            .dailyTarget(MacroTarget.builder()
                .calories(plan.getCaloriesTarget())
                .protein(plan.getProteinTarget())
                .carbs(plan.getCarbsTarget())
                .fats(plan.getFatsTarget())
                .build())
            .days(dayDTOs)
            .generatedByAI(plan.getGeneratedByAI())
            .providerName(plan.getProviderName())
            .model(plan.getModel())
            .createdAt(plan.getCreatedAt())
            .isActive(plan.getIsActive())
            .generatedBy(Map.of(
                "isRealAI",     isRealAI,
                "usedFallback", usedFallback,
                "providerName", plan.getProviderName() != null ? plan.getProviderName() : "Unknown"
            ))
            .build();
    }

    public static HistoryItemDTO toHistoryItem(MealPlan plan) {
        return HistoryItemDTO.builder()
            .id(idToString(plan.getId()))
            .title(plan.getTitle() != null ? plan.getTitle() : "Programme personnalisé")
            .createdAt(plan.getCreatedAt())
            .isActive(Boolean.TRUE.equals(plan.getIsActive()))
            .targetCalories(plan.getCaloriesTarget())
            .durationDays(plan.getDurationDays())
            .build();
    }

    public static MealDTO fromMeal(Meal meal) {
        List<String> altNames = meal.getAlternatives().stream()
            .map(MealAlternative::getName)
            .collect(Collectors.toList());
        List<FoodDTO> foodDTOs = meal.getFoods().stream()
            .map(MealPlanResponseDTO::fromFood)
            .collect(Collectors.toList());

        return MealDTO.builder()
            .id(idToString(meal.getId()))
            .type(meal.getType())
            .name(meal.getName())
            .calories(meal.getCalories())
            .protein(meal.getProtein())
            .carbs(meal.getCarbs())
            .fats(meal.getFats())
            .preparationTime(meal.getPreparationTime())
            .preparation(meal.getPreparation())
            .alternatives(altNames)
            .foods(foodDTOs)
            .orderIndex(meal.getOrderIndex())
            .isCompleted(Boolean.TRUE.equals(meal.getIsCompleted()))
            .build();
    }

    private static DailyPlanDTO fromDailyPlan(DailyPlan day) {
        return DailyPlanDTO.builder()
            .id(idToString(day.getId()))
            .day(day.getDayName())
            .dayNumber(day.getDayNumber())
            .totalCalories(day.getTotalCalories())
            .totalProtein(day.getTotalProtein())
            .totalCarbs(day.getTotalCarbs())
            .totalFats(day.getTotalFats())
            .meals(day.getMeals().stream().map(MealPlanResponseDTO::fromMeal).collect(Collectors.toList()))
            .build();
    }

    private static FoodDTO fromFood(Food food) {
        return FoodDTO.builder()
            .id(idToString(food.getId()))
            .name(food.getName())
            .quantity(food.getQuantity())
            .calories(food.getCalories())
            .protein(food.getProtein())
            .carbs(food.getCarbs())
            .fats(food.getFats())
            .build();
    }

    private static String idToString(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
