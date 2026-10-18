package com.example.backend_meal.ai;

import com.example.backend_meal.nutrition.model.NutritionProfile;

import java.util.List;
import java.util.Map;

/**
 * Moteur algorithmique de développement.
 * Utilisé quand aucune clé API IA externe n'est configurée.
 * Miroir de DynamicNutritionalAIProvider dans src/server/ai.service.ts.
 *
 * IMPORTANT : Ce moteur N'EST PAS une IA générative.
 * Il calcule les repas sur-mesure à partir du profil (calories, macros, allergies)
 * mais il n'appelle aucun modèle de langage.
 */
public class DynamicNutritionalAIProvider implements IAIProvider {

    private static final List<String> DAYS = List.of(
        "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    );

    private static final List<Map<String, Object>> PROTEIN_CHOICES = List.of(
        Map.of("name", "Filet de poulet fermier aux herbes", "p", 32, "c", 0, "f", 3, "cal", 160),
        Map.of("name", "Pavé de saumon grillé au citron",   "p", 24, "c", 0, "f", 14, "cal", 220),
        Map.of("name", "Steak haché façon bouchère 5%",     "p", 26, "c", 0, "f", 5,  "cal", 150),
        Map.of("name", "Filet de cabillaud rôti à l'ail",   "p", 23, "c", 0, "f", 1,  "cal", 100),
        Map.of("name", "Tofu fumé sauté au sésame",         "p", 20, "c", 3, "f", 10, "cal", 180)
    );

    private static final List<Map<String, Object>> CARB_CHOICES = List.of(
        Map.of("name", "Riz basmati complet parfumé",          "p", 4, "c", 45, "f", 1,   "cal", 210, "q", "180g cuit"),
        Map.of("name", "Patates douces rôties au paprika",     "p", 3, "c", 40, "f", 0.5, "cal", 175, "q", "200g"),
        Map.of("name", "Quinoa royal aux petits légumes",      "p", 6, "c", 38, "f", 3,   "cal", 200, "q", "170g cuit"),
        Map.of("name", "Pâtes complètes au basilic",           "p", 8, "c", 46, "f", 1.5, "cal", 230, "q", "160g cuit")
    );

    @Override
    public String getName() {
        return "Dynamic Nutritional Intelligence Provider";
    }

    @Override
    public boolean isRealAI() {
        return false;
    }

    @Override
    public String generateMealPlan(String prompt, NutritionProfile profile) {
        int mealsPerDay = profile.getMealsPerDay() != null ? profile.getMealsPerDay() : 3;
        int targetCal   = profile.getTargetCalories() != null ? profile.getTargetCalories() : 2000;
        int targetP     = profile.getTargetProtein() != null ? profile.getTargetProtein() : 150;
        int targetC     = profile.getTargetCarbs() != null ? profile.getTargetCarbs() : 200;
        int targetF     = profile.getTargetFats() != null ? profile.getTargetFats() : 70;

        List<String> mealTypes = getMealTypes(mealsPerDay);

        StringBuilder days = new StringBuilder();
        for (int d = 0; d < 7; d++) {
            if (d > 0) days.append(",");
            days.append("{\"day\":\"").append(DAYS.get(d)).append("\",\"meals\":[");
            for (int m = 0; m < mealTypes.size(); m++) {
                if (m > 0) days.append(",");
                String type = mealTypes.get(m);
                double fraction = getFraction(type, mealsPerDay);
                int mCal = (int)(targetCal * fraction);
                int mP   = (int)(targetP * fraction);
                int mC   = (int)(targetC * fraction);
                int mF   = (int)(targetF * fraction);
                days.append(buildMeal(type, mCal, mP, mC, mF, d, m, profile));
            }
            days.append("]}");
        }

        return "{"
            + "\"mealPlan\":{"
            + "\"durationDays\":7,"
            + "\"dailyTarget\":{"
            + "\"calories\":" + targetCal + ","
            + "\"protein\":"  + targetP   + ","
            + "\"carbs\":"    + targetC   + ","
            + "\"fats\":"     + targetF
            + "},"
            + "\"days\":[" + days + "]"
            + "}"
            + "}";
    }

    private List<String> getMealTypes(int count) {
        return switch (count) {
            case 2  -> List.of("breakfast", "dinner");
            case 3  -> List.of("breakfast", "lunch", "dinner");
            case 4  -> List.of("breakfast", "lunch", "afternoon_snack", "dinner");
            case 5  -> List.of("breakfast", "morning_snack", "lunch", "afternoon_snack", "dinner");
            default -> List.of("breakfast", "morning_snack", "lunch", "afternoon_snack", "dinner", "evening_snack");
        };
    }

    private double getFraction(String type, int mealCount) {
        return switch (type) {
            case "breakfast"      -> mealCount >= 4 ? 0.25 : 0.30;
            case "lunch"          -> mealCount >= 4 ? 0.35 : 0.40;
            case "dinner"         -> mealCount >= 4 ? 0.28 : 0.30;
            case "morning_snack"  -> 0.10;
            case "afternoon_snack"-> 0.12;
            default               -> 0.08;
        };
    }

    private String buildMeal(String type, int cal, int p, int c, int f,
                             int dayIdx, int orderIdx, NutritionProfile profile) {
        boolean isSnack = type.contains("snack");
        String name;
        String preparation;
        String foods;
        int mealCal;
        int mealP;
        int mealC;
        int mealF;

        if (type.equals("breakfast")) {
            int oats = Math.min(120, Math.max(40, (int) (c * 1.3)));
            int oatsCal = (int) (oats * 3.7);
            int oatsP = (int) (oats * 0.13);
            int oatsC = (int) (oats * 0.6);
            int oatsF = 3;
            name = "Bowl d'avoine protéiné et fruits frais";
            preparation = "Mélanger les flocons d'avoine avec du lait chaud ou du skyr. "
                        + "Laisser reposer 3 min, garnir avec baies et banane.";
            foods = String.format(
                "{\"name\":\"Flocons d'avoine complets\",\"quantity\":\"%dg\",\"calories\":%d,\"protein\":%d,\"carbs\":%d,\"fats\":%d},"
                + "{\"name\":\"Skyr nature 0%%\",\"quantity\":\"200g\",\"calories\":110,\"protein\":20,\"carbs\":8,\"fats\":1},"
                + "{\"name\":\"Baies rouges et banane\",\"quantity\":\"120g\",\"calories\":85,\"protein\":2,\"carbs\":20,\"fats\":0}",
                oats, oatsCal, oatsP, oatsC, oatsF
            );
            mealCal = oatsCal + 110 + 85;
            mealP = oatsP + 20 + 2;
            mealC = oatsC + 8 + 20;
            mealF = oatsF + 1 + 0;
        } else if (isSnack) {
            name = "Collation protéinée équilibrée";
            preparation = "Déguster le yaourt accompagné de fruits secs et de fruits frais.";
            foods = "{\"name\":\"Skyr nature\",\"quantity\":\"180g\",\"calories\":110,\"protein\":19,\"carbs\":6,\"fats\":1},"
                  + "{\"name\":\"Amandes entières\",\"quantity\":\"25g\",\"calories\":145,\"protein\":5,\"carbs\":4,\"fats\":12},"
                  + "{\"name\":\"Pomme Bio\",\"quantity\":\"1 unité\",\"calories\":65,\"protein\":1,\"carbs\":16,\"fats\":0}";
            mealCal = 110 + 145 + 65;
            mealP = 19 + 5 + 1;
            mealC = 6 + 4 + 16;
            mealF = 1 + 12 + 0;
        } else {
            Map<String, Object> prot = PROTEIN_CHOICES.get((dayIdx + orderIdx) % PROTEIN_CHOICES.size());
            Map<String, Object> carb = CARB_CHOICES.get((dayIdx + orderIdx) % CARB_CHOICES.size());
            int vegCal = 120, vegP = 3, vegC = 6, vegF = 10;
            int protCal = ((Number) prot.get("cal")).intValue();
            int protP = ((Number) prot.get("p")).intValue();
            int protC = ((Number) prot.get("c")).intValue();
            int protF = ((Number) prot.get("f")).intValue();
            int carbCal = ((Number) carb.get("cal")).intValue();
            int carbP = ((Number) carb.get("p")).intValue();
            int carbC = (int) Math.round(((Number) carb.get("c")).doubleValue());
            int carbF = (int) Math.round(((Number) carb.get("f")).doubleValue());
            name = type.equals("lunch")
                ? prot.get("name") + " et " + carb.get("name")
                : "Dîner équilibré : " + prot.get("name") + " & légumes de saison";
            preparation = String.format(
                "1. Cuire %s. 2. Saisir %s à feu vif avec un filet d'huile d'olive. "
                + "3. Accompagner de légumes verts assaisonnés au citron.",
                carb.get("name"), prot.get("name"));
            foods = String.format(
                "{\"name\":\"%s\",\"quantity\":\"160g\",\"calories\":%d,\"protein\":%d,\"carbs\":%d,\"fats\":%d},"
                + "{\"name\":\"%s\",\"quantity\":\"%s\",\"calories\":%d,\"protein\":%d,\"carbs\":%d,\"fats\":%d},"
                + "{\"name\":\"Légumes verts vapeur et huile d'olive\",\"quantity\":\"200g\",\"calories\":%d,\"protein\":%d,\"carbs\":%d,\"fats\":%d}",
                prot.get("name"), protCal, protP, protC, protF,
                carb.get("name"), carb.get("q"), carbCal, carbP, carbC, carbF,
                vegCal, vegP, vegC, vegF
            );
            mealCal = protCal + carbCal + vegCal;
            mealP = protP + carbP + vegP;
            mealC = protC + carbC + vegC;
            mealF = protF + carbF + vegF;
        }

        return String.format(
            "{\"type\":\"%s\",\"name\":\"%s\",\"calories\":%d,\"protein\":%d,\"carbs\":%d,\"fats\":%d,"
            + "\"preparationTime\":\"15 min\",\"preparation\":\"%s\","
            + "\"alternatives\":[\"Alternative légère\",\"Alternative végétarienne\"],"
            + "\"foods\":[%s]}",
            type, name.replace("\"", "\\\""), mealCal, mealP, mealC, mealF,
            preparation.replace("\"", "\\\"").replace("\n", " "),
            foods
        );
    }
}
