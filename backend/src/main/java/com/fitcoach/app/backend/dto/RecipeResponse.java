package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private String title;
    private String summary;
    private Integer prepMinutes;
    private Integer cookMinutes;
    private Integer servings;
    private RecipeMacros macros;
    @Builder.Default
    private List<IngredientLine> ingredientsUsed = new ArrayList<>();
    @Builder.Default
    private List<String> steps = new ArrayList<>();
    @Builder.Default
    private List<String> tips = new ArrayList<>();
    @Builder.Default
    private List<String> unusedIngredients = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    private String source;
    private String model;
    private String prompt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeMacros {
        private Integer calories;
        private Double proteinG;
        private Double carbsG;
        private Double fatG;
        private Integer caloriesPerServing;
        private Double proteinPerServingG;
        private Double carbsPerServingG;
        private Double fatPerServingG;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientLine {
        private String name;
        private String foodId;
        private String category;
        private Double quantityG;
        private Integer calories;
        private Double proteinG;
        private Double carbsG;
        private Double fatG;
        private List<String> compatibleDiets;
    }
}
