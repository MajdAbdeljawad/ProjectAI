package com.example.backend_meal.ai;

import com.example.backend_meal.nutrition.model.NutritionProfile;

/**
 * Contrat commun pour tous les fournisseurs IA.
 * Miroir de l'interface TypeScript IAIProvider dans src/server/ai.service.ts.
 */
public interface IAIProvider {

    /** Nom lisible du fournisseur (affiché dans les logs et dans /nutrition/ai-status) */
    String getName();

    /**
     * Génère un programme alimentaire de 7 jours au format JSON structuré.
     * @param prompt  Le prompt personnalisé construit à partir du profil
     * @param profile Le profil nutritionnel complet de l'utilisateur
     * @return        Réponse JSON validée de l'IA
     */
    String generateMealPlan(String prompt, NutritionProfile profile) throws Exception;

    /**
     * @return true si ce provider appelle réellement une API IA générative externe.
     *         false pour le moteur algorithmique de développement.
     */
    boolean isRealAI();
}
