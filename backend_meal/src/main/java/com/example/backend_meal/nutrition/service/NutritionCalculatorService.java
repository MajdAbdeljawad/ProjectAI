package com.example.backend_meal.nutrition.service;

import com.example.backend_meal.nutrition.model.NutritionProfile;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Port Java de NutritionCalculatorService (src/app/core/services/nutrition-calculator.service.ts).
 * Calcule BMR (Mifflin-St Jeor), TDEE et macros selon le profil.
 */
@Service
public class NutritionCalculatorService {

    public NutritionProfile calculate(NutritionProfile profile) {
        if (profile.getWeight() == null || profile.getHeight() == null || profile.getAge() == null) {
            return profile;
        }

        // BMR — Mifflin-St Jeor
        double bmr;
        if ("femme".equalsIgnoreCase(profile.getSex())) {
            bmr = 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * profile.getAge() - 161;
        } else {
            bmr = 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * profile.getAge() + 5;
        }

        // TDEE — multiplicateur activité
        double activityMultiplier = getActivityMultiplier(profile);
        double tdee = bmr * activityMultiplier;

        // Calories cibles selon objectif
        double targetCalories = switch (profile.getGoal() != null ? profile.getGoal() : "maintien") {
            case "perte_de_poids"        -> tdee - 500;
            case "perte_de_poids_rapide" -> tdee - 750;
            case "prise_de_masse"        -> tdee + 350;
            case "prise_de_masse_lente"  -> tdee + 200;
            default                      -> tdee;  // maintien / performances
        };
        targetCalories = Math.max(1200, targetCalories);

        // Macros
        double weightKg = profile.getWeight();
        double proteinMultiplier = switch (profile.getGoal() != null ? profile.getGoal() : "maintien") {
            case "prise_de_masse", "prise_de_masse_lente" -> 2.2;
            case "perte_de_poids", "perte_de_poids_rapide" -> 2.0;
            default -> 1.8;
        };
        int protein = (int)(weightKg * proteinMultiplier);
        int fats    = (int)(targetCalories * 0.25 / 9);
        int carbs   = (int)((targetCalories - protein * 4 - fats * 9) / 4);
        carbs = Math.max(0, carbs);

        profile.setBmr((int) Math.round(bmr));
        profile.setTdee((int) Math.round(tdee));
        profile.setTargetCalories((int) Math.round(targetCalories));
        profile.setTargetProtein(protein);
        profile.setTargetCarbs(carbs);
        profile.setTargetFats(fats);

        return profile;
    }

    private double getActivityMultiplier(NutritionProfile profile) {
        int sessions = profile.getSessionsPerWeek() != null ? profile.getSessionsPerWeek() : 3;
        String level = normalize(profile.getSportsLevel());

        if (sessions <= 1) return 1.2;
        if (sessions <= 3) return "avance".equals(level) ? 1.55 : 1.375;
        if (sessions <= 5) return "avance".equals(level) ? 1.725 : 1.55;
        return "avance".equals(level) ? 1.9 : 1.725;
    }

    /** Maps Angular values like "avancé" / "intermédiaire" to ASCII keys. */
    private String normalize(String value) {
        if (value == null || value.isBlank()) return "intermediaire";
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return stripped.toLowerCase(Locale.ROOT).trim();
    }
}
