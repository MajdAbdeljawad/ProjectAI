package com.example.backend_meal.nutrition.service;

import com.example.backend_meal.nutrition.exception.ValidationException;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates the raw parsed JSON from OpenAI before persistence.
 *
 * 4 validation levels:
 *   L1 — Structure (7 days, meals present, foods present)
 *   L2 — Food values (numeric, >= 0)
 *   L3 — Meal totals recalculated from foods (source of truth)
 *   L4 — Allergen / dietary restriction checking
 */
@Component
public class MealPlanValidator {

    private static final int CALORIE_TOLERANCE  = 50;   // kcal
    private static final int MACRO_TOLERANCE_G  = 5;    // grams

    // Allergen synonym map — key = canonical, values = terms to detect in food names
    private static final Map<String, List<String>> ALLERGEN_SYNONYMS = new LinkedHashMap<>();

    static {
        ALLERGEN_SYNONYMS.put("arachides",  List.of("arachide", "arachides", "cacahuète", "cacahuètes", "peanut", "peanuts", "cacahuete", "cacahuetes"));
        ALLERGEN_SYNONYMS.put("peanuts",    List.of("arachide", "arachides", "cacahuète", "cacahuètes", "peanut", "peanuts", "cacahuete", "cacahuetes"));
        ALLERGEN_SYNONYMS.put("gluten",     List.of("gluten", "blé", "ble", "wheat", "farine", "seigle", "orge", "pain", "pâtes", "pates", "baguette", "semoule"));
        ALLERGEN_SYNONYMS.put("lactose",    List.of("lait", "lactose", "dairy", "milk", "fromage", "cheese", "crème", "creme", "beurre", "yaourt", "yogurt", "skyr"));
        ALLERGEN_SYNONYMS.put("gluten-free",List.of("gluten", "blé", "ble", "wheat", "farine de blé", "seigle", "orge"));
        ALLERGEN_SYNONYMS.put("nuts",       List.of("noix", "amande", "noisette", "noix de cajou", "pistache", "pecan", "macadamia", "nut"));
        ALLERGEN_SYNONYMS.put("soja",       List.of("soja", "soy", "tofu", "edamame"));
        ALLERGEN_SYNONYMS.put("soy",        List.of("soja", "soy", "tofu", "edamame"));
        ALLERGEN_SYNONYMS.put("oeufs",      List.of("oeuf", "oeufs", "egg", "eggs"));
        ALLERGEN_SYNONYMS.put("eggs",       List.of("oeuf", "oeufs", "egg", "eggs"));
        ALLERGEN_SYNONYMS.put("poisson",    List.of("poisson", "saumon", "cabillaud", "thon", "sardine", "fish", "truite"));
        ALLERGEN_SYNONYMS.put("fish",       List.of("poisson", "saumon", "cabillaud", "thon", "sardine", "fish", "truite"));
        ALLERGEN_SYNONYMS.put("fruits de mer", List.of("crevette", "homard", "crabe", "moule", "huître", "scallop", "shrimp", "seafood"));
        ALLERGEN_SYNONYMS.put("shellfish",  List.of("crevette", "homard", "crabe", "moule", "huître", "scallop", "shrimp", "seafood"));
    }

    private static final List<String> MEAT_TERMS = List.of(
        "poulet", "boeuf", "porc", "veau", "agneau", "dinde", "canard", "viande",
        "steak", "jambon", "saucisse", "merguez", "chicken", "beef", "pork", "lamb", "turkey"
    );
    private static final List<String> FISH_TERMS = List.of(
        "saumon", "cabillaud", "thon", "sardine", "truite", "maquereau", "fish", "poisson"
    );
    private static final List<String> ANIMAL_TERMS = List.of(
        "viande", "poulet", "boeuf", "porc", "agneau", "saumon", "cabillaud", "thon",
        "fromage", "lait", "beurre", "crème", "yaourt", "skyr", "oeuf", "honey", "miel"
    );

    /**
     * Full validation of the parsed AI response.
     * Throws ValidationException with a clear message if anything is wrong.
     */
    public void validate(Map<String, Object> parsed, NutritionProfile profile) {
        // L1 — Structure
        validateStructure(parsed);

        // L2+L3 — Food values + meal totals (recalculate)
        validateAndRecalculate(parsed);

        // L4 — Allergens + Dietary restrictions
        List<String> violations = findAllergenViolations(parsed, profile);
        if (!violations.isEmpty()) {
            throw new ValidationException("Violations d'allergènes détectées: " + String.join(", ", violations));
        }
        validateDietaryRestrictions(parsed, profile);
    }

    /** Validates JSON structure: mealPlan key, 7 days, meals, foods. */
    @SuppressWarnings("unchecked")
    private void validateStructure(Map<String, Object> parsed) {
        if (!parsed.containsKey("mealPlan")) {
            throw new ValidationException("Réponse IA invalide: clé 'mealPlan' absente.");
        }
        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("mealPlan");
        if (mealPlan == null) {
            throw new ValidationException("Réponse IA invalide: 'mealPlan' est null.");
        }

        List<?> days = (List<?>) mealPlan.get("days");
        if (days == null || days.isEmpty()) {
            throw new ValidationException("Réponse IA invalide: 'days' absent ou vide.");
        }
        if (days.size() != 7) {
            throw new ValidationException(
                "Réponse IA invalide: exactement 7 jours requis, reçu: " + days.size());
        }

        for (int d = 0; d < days.size(); d++) {
            Map<String, Object> day = (Map<String, Object>) days.get(d);
            if (day == null) throw new ValidationException("Jour " + (d+1) + " est null.");

            List<?> meals = (List<?>) day.get("meals");
            if (meals == null || meals.isEmpty()) {
                throw new ValidationException("Jour " + (d+1) + ": 'meals' absent ou vide.");
            }

            for (int m = 0; m < meals.size(); m++) {
                Map<String, Object> meal = (Map<String, Object>) meals.get(m);
                if (meal == null) throw new ValidationException("Jour " + (d+1) + " repas " + (m+1) + " est null.");
                if (meal.get("name") == null || meal.get("name").toString().isBlank()) {
                    throw new ValidationException("Jour " + (d+1) + " repas " + (m+1) + ": nom manquant.");
                }

                List<?> foods = (List<?>) meal.get("foods");
                if (foods == null || foods.isEmpty()) {
                    throw new ValidationException("Jour " + (d+1) + " repas '" + meal.get("name") + "': 'foods' absent ou vide.");
                }

                for (int f = 0; f < foods.size(); f++) {
                    Map<String, Object> food = (Map<String, Object>) foods.get(f);
                    if (food == null || food.get("name") == null) {
                        throw new ValidationException("Jour " + (d+1) + " repas " + (m+1) + " aliment " + (f+1) + ": nom manquant.");
                    }
                    // calories must be a number
                    Object cal = food.get("calories");
                    if (cal == null) throw new ValidationException("Aliment '" + food.get("name") + "': calories manquantes.");
                    int calInt = toInt(cal, food.get("name") + ".calories");
                    if (calInt < 0) throw new ValidationException("Aliment '" + food.get("name") + "': calories négatives (" + calInt + ").");
                }
            }
        }
    }

    /**
     * Recalculates meal totals from foods (source of truth).
     * Logs discrepancies and REPLACES GPT values with computed ones.
     */
    @SuppressWarnings("unchecked")
    public void validateAndRecalculate(Map<String, Object> parsed) {
        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("mealPlan");
        List<Map<String, Object>> days = (List<Map<String, Object>>) mealPlan.get("days");

        for (Map<String, Object> day : days) {
            List<Map<String, Object>> meals = (List<Map<String, Object>>) day.get("meals");
            int dayCal = 0, dayProt = 0, dayCarbs = 0, dayFats = 0;

            for (Map<String, Object> meal : meals) {
                List<Map<String, Object>> foods = (List<Map<String, Object>>) meal.get("foods");

                int sumCal = 0, sumProt = 0, sumCarbs = 0, sumFats = 0;
                for (Map<String, Object> food : foods) {
                    sumCal   += toInt(food.get("calories"), "calories");
                    sumProt  += toInt(food.get("protein"),  "protein");
                    sumCarbs += toInt(food.get("carbs"),    "carbs");
                    sumFats  += toInt(food.get("fats"),     "fats");
                }

                int gptCal = toInt(meal.get("calories"), "meal.calories");
                if (Math.abs(gptCal - sumCal) > CALORIE_TOLERANCE) {
                    System.out.printf("[MealPlanValidator] ⚠ Incohérence: %s GPT=%d kcal, recalculé=%d kcal — valeur recalculée utilisée%n",
                        meal.get("name"), gptCal, sumCal);
                }
                // OVERRIDE with recalculated values (source of truth = foods)
                meal.put("calories", sumCal);
                meal.put("protein",  sumProt);
                meal.put("carbs",    sumCarbs);
                meal.put("fats",     sumFats);

                dayCal   += sumCal;
                dayProt  += sumProt;
                dayCarbs += sumCarbs;
                dayFats  += sumFats;
            }
            // Also set day totals
            day.put("totalCalories", dayCal);
            day.put("totalProtein",  dayProt);
            day.put("totalCarbs",    dayCarbs);
            day.put("totalFats",     dayFats);
        }
    }

    /** Returns list of allergen violation descriptions found in any food/meal name. */
    @SuppressWarnings("unchecked")
    public List<String> findAllergenViolations(Map<String, Object> parsed, NutritionProfile profile) {
        List<String> violations = new ArrayList<>();
        if (profile.getAllergies() == null || profile.getAllergies().isEmpty()) return violations;

        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("mealPlan");
        List<Map<String, Object>> days = (List<Map<String, Object>>) mealPlan.get("days");

        Set<String> termsToCheck = new LinkedHashSet<>();
        for (String allergen : profile.getAllergies()) {
            String key = allergen.toLowerCase().trim();
            List<String> synonyms = ALLERGEN_SYNONYMS.get(key);
            if (synonyms != null) {
                termsToCheck.addAll(synonyms);
            } else {
                termsToCheck.add(key); // check as-is
            }
        }

        for (Map<String, Object> day : days) {
            List<Map<String, Object>> meals = (List<Map<String, Object>>) day.get("meals");
            for (Map<String, Object> meal : meals) {
                String mealName = meal.get("name") != null ? meal.get("name").toString().toLowerCase() : "";
                checkTerms(mealName, termsToCheck, "repas '" + meal.get("name") + "'", violations);

                List<Map<String, Object>> foods = (List<Map<String, Object>>) meal.get("foods");
                if (foods != null) {
                    for (Map<String, Object> food : foods) {
                        String foodName = food.get("name") != null ? food.get("name").toString().toLowerCase() : "";
                        checkTerms(foodName, termsToCheck, "aliment '" + food.get("name") + "'", violations);
                    }
                }
            }
        }
        return violations;
    }

    /** Validates dietary restrictions (vegetarian, vegan). */
    @SuppressWarnings("unchecked")
    private void validateDietaryRestrictions(Map<String, Object> parsed, NutritionProfile profile) {
        String diet = profile.getDiet() != null ? profile.getDiet().toLowerCase() : "";
        if (!diet.contains("vegetar") && !diet.contains("vegan")) return;

        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("mealPlan");
        List<Map<String, Object>> days = (List<Map<String, Object>>) mealPlan.get("days");
        List<String> forbidden = diet.contains("vegan") ? ANIMAL_TERMS : MEAT_TERMS;
        String label = diet.contains("vegan") ? "Vegan" : "Végétarien";

        for (Map<String, Object> day : days) {
            List<Map<String, Object>> meals = (List<Map<String, Object>>) day.get("meals");
            for (Map<String, Object> meal : meals) {
                List<Map<String, Object>> foods = (List<Map<String, Object>>) meal.get("foods");
                if (foods == null) continue;
                for (Map<String, Object> food : foods) {
                    String fn = food.get("name") != null ? food.get("name").toString().toLowerCase() : "";
                    for (String term : forbidden) {
                        if (fn.contains(term)) {
                            throw new ValidationException(
                                label + ": aliment interdit détecté: '" + food.get("name") + "' contient '" + term + "'");
                        }
                    }
                }
            }
        }
    }

    private void checkTerms(String text, Set<String> terms, String context, List<String> violations) {
        for (String term : terms) {
            if (text.contains(term)) {
                violations.add(context + " contient '" + term + "'");
                return; // one match per food/meal is enough to flag
            }
        }
    }

    private int toInt(Object val, String fieldName) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString().trim()); }
        catch (NumberFormatException e) {
            System.err.println("[MealPlanValidator] Valeur non numérique pour " + fieldName + ": '" + val + "' → 0");
            return 0;
        }
    }
}
