package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecipeRequest;
import com.fitcoach.app.backend.dto.RecipeRequest.IngredientInput;
import com.fitcoach.app.backend.dto.RecipeResponse;
import com.fitcoach.app.backend.service.RecipeMacroService.MacroComputation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeMacroServiceTest {

    private FoodNutritionCatalog catalog;
    private RecipeMacroService service;

    @BeforeEach
    void setUp() throws Exception {
        catalog = new FoodNutritionCatalog();
        catalog.load();
        service = new RecipeMacroService(catalog);
    }

    @Test
    void catalogContainsAtLeastFortyFoods() {
        assertThat(catalog.size()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void calculatesMacrosFromKnownIngredients() {
        RecipeRequest request = request("omnivore", 2,
                ingredient("chicken breast", 150.0),
                ingredient("rice", 200.0),
                ingredient("olive oil", 10.0));

        MacroComputation result = service.compute(request);

        // chicken 150g: 247.5 kcal, 46.5p, 5.4f ; rice 200g: 260 kcal, 5.4p, 56c, 0.6f ; oil 10g: 88.4 kcal, 10f
        assertThat(result.macros().getCalories()).isEqualTo(596);
        assertThat(result.macros().getProteinG()).isEqualTo(51.9);
        assertThat(result.macros().getCarbsG()).isEqualTo(56.0);
        assertThat(result.macros().getFatG()).isEqualTo(16.0);
        assertThat(result.macros().getCaloriesPerServing()).isEqualTo(298);
        assertThat(result.macros().getProteinPerServingG()).isEqualTo(26.0);
        assertThat(result.used()).hasSize(3);
        assertThat(result.unknown()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void defaultsQuantityTo100gWhenMissing() {
        RecipeRequest request = request("omnivore", 1, ingredient("banana", null));

        MacroComputation result = service.compute(request);

        assertThat(result.used()).hasSize(1);
        assertThat(result.used().get(0).getQuantityG()).isEqualTo(100.0);
        assertThat(result.macros().getCalories()).isEqualTo(89);
        assertThat(result.macros().getCarbsG()).isEqualTo(23.0);
    }

    @Test
    void unknownFoodIsIgnoredWithWarning() {
        RecipeRequest request = request("omnivore", 1,
                ingredient("chicken breast", 100.0),
                ingredient("dragon fruit xyz", 100.0));

        MacroComputation result = service.compute(request);

        assertThat(result.used()).hasSize(1);
        assertThat(result.unknown()).containsExactly("dragon fruit xyz");
        assertThat(result.warnings()).anyMatch(w -> w.contains("Unknown food"));
        assertThat(result.macros().getCalories()).isEqualTo(165);
    }

    @Test
    void veganDietRejectsChicken() {
        RecipeRequest request = request("vegan", 1,
                ingredient("chicken", 100.0),
                ingredient("tofu", 150.0),
                ingredient("rice", 150.0));

        MacroComputation result = service.compute(request);

        assertThat(result.used()).extracting(line -> line.getFoodId())
                .containsExactly("tofu", "rice_cooked");
        assertThat(result.incompatible()).contains("chicken");
        assertThat(result.warnings()).anyMatch(w -> w.contains("not compatible with vegan"));
    }

    @Test
    void frenchAliasIsResolved() {
        assertThat(catalog.find("poulet")).isPresent();
        assertThat(catalog.find("avoine").orElseThrow().getId()).isEqualTo("oats");
        assertThat(catalog.find("yaourt grec").orElseThrow().getId()).isEqualTo("greek_yogurt");
    }

    @Test
    void deterministicResponseExposesPerServingMacros() {
        RecipeRequest request = request("halal", 2,
                ingredient("saumon", 120.0),
                ingredient("pomme de terre", 200.0));

        RecipeResponse response = service.toDeterministicResponse(request, service.compute(request));

        assertThat(response.getSource()).isEqualTo("deterministic");
        assertThat(response.getServings()).isEqualTo(2);
        assertThat(response.getMacros().getCaloriesPerServing()).isPositive();
        assertThat(response.getIngredientsUsed()).isNotEmpty();
        assertThat(response.getUnusedIngredients()).isEmpty();
    }

    private IngredientInput ingredient(String name, Double quantityG) {
        IngredientInput input = new IngredientInput();
        input.setName(name);
        input.setQuantityG(quantityG);
        return input;
    }

    private RecipeRequest request(String diet, int servings, IngredientInput... ingredients) {
        RecipeRequest request = new RecipeRequest();
        request.setFirstName("Oussama");
        request.setDietType(diet);
        request.setServings(servings);
        request.setGoal("muscle gain");
        request.setIngredients(new ArrayList<>(List.of(ingredients)));
        return request;
    }
}
