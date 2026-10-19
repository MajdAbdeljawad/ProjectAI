package com.fitcoach.app.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RecipeRequest {
    @NotBlank
    private String firstName;

    @NotNull
    @Size(min = 1, max = 20)
    private List<@Valid @NotNull IngredientInput> ingredients;

    @NotBlank
    private String dietType;

    private String allergies;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer servings;

    @NotBlank
    private String goal;

    @Min(10)
    @Max(120)
    private Integer maxMinutes;

    private String equipment;

    @Data
    public static class IngredientInput {
        @NotBlank
        @Size(max = 80)
        private String name;

        @DecimalMin("10")
        @DecimalMax("2000")
        private Double quantityG;
    }
}
