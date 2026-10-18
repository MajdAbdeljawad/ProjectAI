package com.example.backend_meal.ai;

import com.example.backend_meal.config.AIProperties;
import com.example.backend_meal.nutrition.model.NutritionProfile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrateur IA — sélectionne le provider, construit le prompt, gère le retry.
 *
 * Provider selection:
 *   AI_API_KEY présente + AI_PROVIDER=gemini → GeminiProvider
 *   AI_API_KEY présente + autre             → OpenAICompatibleProvider
 *   AI_API_KEY absente                      → DynamicNutritionalAIProvider (fallback dev)
 *
 * Retry policy: up to MAX_RETRIES additional attempts if OpenAI returns invalid JSON.
 * If all attempts fail: throws exception — does NOT silently fall back to Dynamic.
 *
 * La clé API n'est JAMAIS transmise au frontend Angular.
 */
@Service
public class AIService {

    private static final int MAX_RETRIES = 2; // max 2 additional retries (3 total)

    private final IAIProvider provider;
    private final AIProperties props;

    public AIService(AIProperties props) {
        this.props = props;
        this.provider = createProvider(props);

        if (provider.isRealAI()) {
            System.out.println("[AIService] ✅ IA générative ACTIVE : " + provider.getName());
        } else {
            System.out.println("[AIService] ⚠️  Mode DÉVELOPPEMENT : " + provider.getName()
                + " — Ce moteur n'est PAS une IA générative.");
        }
    }

    private IAIProvider createProvider(AIProperties p) {
        if (!p.hasRealKey()) {
            return new DynamicNutritionalAIProvider();
        }
        if (p.isGemini()) {
            String geminiUrl = (p.getApiUrl() != null && !p.getApiUrl().isBlank())
                ? p.getApiUrl()
                : "https://generativelanguage.googleapis.com/v1beta/models";
            String geminiModel = (p.getModel() != null && !p.getModel().isBlank())
                ? p.getModel()
                : "gemini-1.5-flash";
            return new GeminiProvider(p.getApiKey(), geminiUrl, geminiModel);
        }
        String openaiUrl = (p.getApiUrl() != null && !p.getApiUrl().isBlank())
            ? p.getApiUrl()
            : "https://api.openai.com/v1";
        String openaiModel = (p.getModel() != null && !p.getModel().isBlank())
            ? p.getModel()
            : "gpt-4o-mini";
        return new OpenAICompatibleProvider(p.getApiKey(), openaiUrl, openaiModel);
    }

    public String getProviderName() { return provider.getName(); }
    public boolean isRealAI()       { return provider.isRealAI(); }
    public String getModel()        { return props.getModel() != null ? props.getModel() : "gpt-4o-mini"; }

    /**
     * Builds the personalized prompt with 25+ profile variables.
     * Includes explicit nutritional coherence instructions for OpenAI.
     */
    public String buildPrompt(NutritionProfile profile) {
        String allergies    = joinOrNone(profile.getAllergies());
        String intolerances = joinOrNone(profile.getIntolerances());
        String allergyBlock = (allergies.equals("Aucune") && intolerances.equals("Aucune"))
            ? "Aucune"
            : allergies + (intolerances.equals("Aucune") ? "" : ", " + intolerances);

        return """
            Tu es un diététicien-nutritionniste diplômé spécialisé en sport de haut niveau.
            Génère un programme alimentaire personnalisé complet de 7 jours (Lundi à Dimanche).

            === DONNÉES DU SPORTIF ===
            - Nom : %s
            - Âge : %s ans | Sexe : %s
            - Taille : %s cm | Poids : %s kg
            - Niveau sportif : %s | Séances/semaine : %s
            - Type d'entraînement : %s
            - Objectif : %s
            - Régime alimentaire : %s
            - Aliments préférés : %s
            - Aliments INTERDITS (ne jamais inclure) : %s
            - ALLERGIES/INTOLÉRANCES STRICTES (DANGER — AUCUNE TRACE TOLÉRÉE) : %s
            - Restrictions alimentaires : %s
            - Repas/jour : %s | Réveil : %s | Coucher : %s
            - Budget : %s | Temps cuisine : %s

            === CIBLES NUTRITIONNELLES CALCULÉES ===
            - BMR : %s kcal | TDEE : %s kcal
            - Cible calorique : %s kcal/jour
            - Protéines : %s g/jour | Glucides : %s g/jour | Lipides : %s g/jour

            === RÈGLES ABSOLUES ===
            1. Génère exactement 7 jours: "Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche".
            2. Chaque jour doit comporter exactement %s repas.
            3. JAMAIS d'aliments contenant les allergènes déclarés: %s
            4. meal.calories DOIT égaler la somme exacte des food.calories du repas.
            5. meal.protein DOIT égaler la somme exacte des food.protein du repas.
            6. meal.carbs DOIT égaler la somme exacte des food.carbs du repas.
            7. meal.fats DOIT égaler la somme exacte des food.fats du repas.
            8. calories, protein, carbs, fats sont des entiers (pas de chaînes comme "40g").
            9. Réponds UNIQUEMENT en JSON valide. Pas de Markdown. Pas de texte avant ou après.

            === FORMAT JSON OBLIGATOIRE ===
            {
              "mealPlan": {
                "durationDays": 7,
                "dailyTarget": {"calories":%s,"protein":%s,"carbs":%s,"fats":%s},
                "days": [
                  {
                    "day": "Lundi",
                    "meals": [
                      {
                        "type": "breakfast",
                        "name": "Nom du repas",
                        "calories": 500,
                        "protein": 35,
                        "carbs": 60,
                        "fats": 15,
                        "preparationTime": "15 min",
                        "preparation": "Instructions de préparation détaillées.",
                        "alternatives": ["Alternative 1", "Alternative 2"],
                        "foods": [
                          {"name":"Aliment","quantity":"100g","calories":200,"protein":15,"carbs":25,"fats":5}
                        ]
                      }
                    ]
                  }
                ]
              }
            }
            """.formatted(
                str(profile.getFullName()), str(profile.getAge()), str(profile.getSex()),
                str(profile.getHeight()), str(profile.getWeight()),
                str(profile.getSportsLevel()), str(profile.getSessionsPerWeek()),
                str(profile.getWorkoutType()), str(profile.getGoal()), str(profile.getDiet()),
                joinOrNone(profile.getPreferredFoods()), joinOrNone(profile.getDislikedFoods()),
                allergyBlock, joinOrNone(profile.getDietaryRestrictions()),
                str(profile.getMealsPerDay()), str(profile.getWakeUpTime()), str(profile.getSleepTime()),
                str(profile.getBudget()), str(profile.getCookingTime()),
                str(profile.getBmr()), str(profile.getTdee()),
                str(profile.getTargetCalories()), str(profile.getTargetProtein()),
                str(profile.getTargetCarbs()), str(profile.getTargetFats()),
                str(profile.getMealsPerDay()), allergyBlock,
                str(profile.getTargetCalories()), str(profile.getTargetProtein()),
                str(profile.getTargetCarbs()), str(profile.getTargetFats())
            );
    }

    /**
     * Generates meal plan with retry logic.
     *
     * - If provider is REAL AI: up to MAX_RETRIES additional attempts on failure.
     *   After all retries exhausted: throws exception — does NOT silently use Dynamic.
     * - If provider is Dynamic (fallback): single attempt, no retry needed.
     *
     * Allergen validation is performed here as a raw text pre-check.
     * Full structural validation is done by MealPlanValidator after JSON parsing.
     */
    public String generatePlan(NutritionProfile profile) throws Exception {
        String prompt = buildPrompt(profile);
        Exception lastException = null;

        int maxAttempts = provider.isRealAI() ? (1 + MAX_RETRIES) : 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.printf("[AIService] Tentative %d/%d — Fournisseur: %s%n",
                    attempt, maxAttempts, provider.getName());

                String result = provider.generateMealPlan(prompt, profile);

                // Raw allergen check before returning to service for full parsing
                validateAllergens(result, profile);

                System.out.println("[AIService] ✅ Génération réussie à la tentative " + attempt);
                return result;

            } catch (Exception e) {
                lastException = e;
                System.err.printf("[AIService] ❌ Tentative %d échouée: %s%n", attempt, e.getMessage());
                if (attempt < maxAttempts) {
                    System.out.println("[AIService] 🔄 Nouvelle tentative dans 1 seconde...");
                    Thread.sleep(1000);
                }
            }
        }

        // All attempts failed — do NOT silently fall back to Dynamic if real AI was configured
        String errorMsg = String.format(
            "Échec après %d tentative(s) avec %s: %s",
            maxAttempts, provider.getName(),
            lastException != null ? lastException.getMessage() : "erreur inconnue"
        );
        System.err.println("[AIService] 🚫 " + errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }

    /**
     * Raw text allergen check — catches obvious violations before JSON parsing.
     */
    private void validateAllergens(String jsonResponse, NutritionProfile profile) {
        if (profile.getAllergies() == null || profile.getAllergies().isEmpty()) return;
        String lower = jsonResponse.toLowerCase();
        for (String allergen : profile.getAllergies()) {
            if (!allergen.isBlank() && lower.contains(allergen.toLowerCase().trim())) {
                throw new RuntimeException(
                    "Violation d'allergène détectée dans la réponse brute: \"" + allergen + "\"");
            }
        }
    }

    private String str(Object val) {
        return val != null ? val.toString() : "Non spécifié";
    }

    private String joinOrNone(List<String> list) {
        if (list == null || list.isEmpty()) return "Aucune";
        return String.join(", ", list);
    }
}
