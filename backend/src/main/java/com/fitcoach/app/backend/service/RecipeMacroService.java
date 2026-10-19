package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecipeRequest;
import com.fitcoach.app.backend.dto.RecipeRequest.IngredientInput;
import com.fitcoach.app.backend.dto.RecipeResponse;
import com.fitcoach.app.backend.dto.RecipeResponse.IngredientLine;
import com.fitcoach.app.backend.dto.RecipeResponse.RecipeMacros;
import com.fitcoach.app.backend.service.FoodNutritionCatalog.FoodEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipeMacroService {

    private final FoodNutritionCatalog catalog;

    public MacroComputation compute(RecipeRequest request) {
        List<IngredientLine> used = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        List<String> incompatible = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        double calories = 0;
        double protein = 0;
        double carbs = 0;
        double fat = 0;

        for (IngredientInput input : request.getIngredients()) {
            double quantity = input.getQuantityG() == null
                    ? FoodNutritionCatalog.DEFAULT_QUANTITY_G
                    : input.getQuantityG();
            Optional<FoodEntry> match = catalog.find(input.getName());
            if (match.isEmpty()) {
                unknown.add(input.getName());
                warnings.add("Unknown food ignored (no nutrition data): " + input.getName());
                continue;
            }

            FoodEntry food = match.get();
            if (!catalog.isCompatible(food, request.getDietType())) {
                incompatible.add(input.getName());
                warnings.add("Food ignored (not compatible with " + request.getDietType() + " diet): " + input.getName());
                continue;
            }

            double factor = quantity / 100.0;
            double lineKcal = food.getKcal() * factor;
            double lineProtein = food.getProtein() * factor;
            double lineCarbs = food.getCarbs() * factor;
            double lineFat = food.getFat() * factor;

            calories += lineKcal;
            protein += lineProtein;
            carbs += lineCarbs;
            fat += lineFat;

            used.add(IngredientLine.builder()
                    .name(displayName(input.getName(), food))
                    .foodId(food.getId())
                    .category(food.getCategory())
                    .quantityG(round(quantity, 1))
                    .calories((int) Math.round(lineKcal))
                    .proteinG(round(lineProtein, 1))
                    .carbsG(round(lineCarbs, 1))
                    .fatG(round(lineFat, 1))
                    .compatibleDiets(food.getDiets())
                    .build());
        }

        if (used.isEmpty()) {
            warnings.add("No usable ingredients with known nutrition data for this diet.");
        }

        int servings = Math.max(1, request.getServings());
        RecipeMacros macros = RecipeMacros.builder()
                .calories((int) Math.round(calories))
                .proteinG(round(protein, 1))
                .carbsG(round(carbs, 1))
                .fatG(round(fat, 1))
                .caloriesPerServing((int) Math.round(calories / servings))
                .proteinPerServingG(round(protein / servings, 1))
                .carbsPerServingG(round(carbs / servings, 1))
                .fatPerServingG(round(fat / servings, 1))
                .build();

        return new MacroComputation(macros, used, unknown, incompatible, warnings);
    }

    public RecipeResponse toDeterministicResponse(RecipeRequest request, MacroComputation computation) {
        return RecipeResponse.builder()
                .title("Macro breakdown for " + request.getFirstName())
                .summary(computation.used().isEmpty()
                        ? "No matching foods were found in the nutrition table."
                        : "Macros calculated from the nutrition table for " + computation.used().size()
                        + " ingredient(s). AI recipe generation can be added on top of these numbers.")
                .servings(request.getServings())
                .macros(computation.macros())
                .ingredientsUsed(computation.used())
                .unusedIngredients(computation.unknown())
                .warnings(computation.warnings())
                .source("deterministic")
                .build();
    }

    private String displayName(String raw, FoodEntry food) {
        return raw == null || raw.isBlank()
                ? food.getId().replace('_', ' ')
                : Character.toUpperCase(raw.trim().charAt(0)) + raw.trim().substring(1).toLowerCase(Locale.ROOT);
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public record MacroComputation(
            RecipeMacros macros,
            List<IngredientLine> used,
            List<String> unknown,
            List<String> incompatible,
            List<String> warnings
    ) {}
}
