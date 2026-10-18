import { Router, Request, Response } from 'express';
import { DatabaseService } from './database';
import { AIService } from './ai.service';
import { NutritionCalculatorService } from '../app/core/services/nutrition-calculator.service';
import { MealPlan, NutritionProfile } from '../app/core/models/nutrition.models';

export function createNutritionRouter(): Router {
  const router = Router();
  const db = DatabaseService.getInstance();
  const aiService = new AIService();
  const calculator = new NutritionCalculatorService();

  // Helper to extract authenticated/active user ID
  const getUserId = (req: Request): string => {
    const customUser = req.headers['x-user-id'] as string || req.query['userId'] as string;
    return customUser && customUser.trim() ? customUser.trim() : 'default_user_1';
  };

  /**
   * GET /nutrition/profile
   * Récupère le profil nutritionnel de l'utilisateur
   */
  router.get('/profile', (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const profile = db.getProfile(userId);
      if (!profile) {
        return res.status(404).json({ error: 'Profil introuvable pour cet utilisateur' });
      }
      return res.json(profile);
    } catch (err: any) {
      console.error('[GET /profile] Error:', err);
      return res.status(500).json({ error: 'Erreur interne du serveur lors de la récupération du profil.' });
    }
  });

  /**
   * Helper pour mettre à jour ou créer un profil
   */
  const handleSaveProfile = (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const body = req.body;

      // Validation des champs essentiels
      if (body.age !== undefined && (isNaN(Number(body.age)) || Number(body.age) <= 0 || Number(body.age) > 120)) {
        return res.status(400).json({ error: 'L\'âge doit être un nombre valide entre 1 et 120.' });
      }
      if (body.weight !== undefined && (isNaN(Number(body.weight)) || Number(body.weight) < 30 || Number(body.weight) > 300)) {
        return res.status(400).json({ error: 'Le poids doit être un nombre valide entre 30 et 300 kg.' });
      }
      if (body.height !== undefined && (isNaN(Number(body.height)) || Number(body.height) < 100 || Number(body.height) > 250)) {
        return res.status(400).json({ error: 'La taille doit être un nombre valide entre 100 et 250 cm.' });
      }

      // Recalcul automatique des besoins nutritionnels pour garantir la cohérence scientifique
      const calculations = calculator.calculateFullNeeds(body);
      const enrichedData: Partial<NutritionProfile> = {
        ...body,
        bmr: calculations.bmr,
        tdee: calculations.tdee,
        targetCalories: calculations.targetCalories,
        targetProtein: calculations.macros.protein,
        targetCarbs: calculations.macros.carbs,
        targetFats: calculations.macros.fats
      };

      const saved = db.saveOrUpdateProfile(userId, enrichedData);
      return res.status(200).json(saved);
    } catch (err: any) {
      console.error('[Save Profile] Error:', err);
      return res.status(500).json({ error: 'Erreur lors de la sauvegarde du profil en base de données.' });
    }
  };

  router.post('/profile', handleSaveProfile);
  router.put('/profile', handleSaveProfile);
  router.patch('/profile', handleSaveProfile);

  /**
   * POST /nutrition/calculate
   * Calcul en temps réel des besoins sans persister
   */
  router.post('/calculate', (req: Request, res: Response) => {
    try {
      const calculation = calculator.calculateFullNeeds(req.body);
      return res.json(calculation);
    } catch (err: any) {
      return res.status(400).json({ error: err.message });
    }
  });

  /**
   * GET /nutrition/ai-status
   * Indique si une vraie API IA générative est configurée côté serveur.
   * La clé API n'est JAMAIS renvoyée au frontend.
   */
  router.get('/ai-status', (_req: Request, res: Response) => {
    const isReal = aiService.isRealAI;
    const providerName = aiService.getProviderName();
    return res.json({
      isRealAI: isReal,
      providerName,
      message: isReal
        ? `Fournisseur IA générative actif : ${providerName}`
        : 'Aucune clé API IA configurée. Mode développement algorithmique actif — les repas ne sont PAS générés par une IA générative.'
    });
  });

  /**
   * POST /nutrition/meal-plan/generate
   * Génération par IA d'un nouveau programme de 7 jours
   */
  router.post('/meal-plan/generate', async (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const profile = db.getProfile(userId);

      if (!profile) {
        return res.status(404).json({ 
          error: 'Impossible de générer un programme sans profil. Veuillez d\'abord enregistrer votre profil nutritionnel.' 
        });
      }

      const isRealAI = aiService.isRealAI;
      const providerName = aiService.getProviderName();

      console.log(`[Generate Meal Plan] Début de génération pour ${profile.fullName} (${profile.goal}, ${profile.targetCalories} kcal)`);
      console.log(`[Generate Meal Plan] Fournisseur : ${providerName} | IA réelle : ${isRealAI}`);

      // Appel de l'orchestrateur IA
      const aiResponse = await aiService.generatePlan(profile);

      // Structurer et persister le programme en base
      const newPlan: MealPlan = {
        id: `plan_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
        userId,
        title: `Programme personnalisé - ${profile.goal.replace(/_/g, ' ').toUpperCase()}`,
        durationDays: aiResponse.mealPlan.durationDays || 7,
        dailyTarget: aiResponse.mealPlan.dailyTarget || {
          calories: profile.targetCalories,
          protein: profile.targetProtein,
          carbs: profile.targetCarbs,
          fats: profile.targetFats
        },
        days: aiResponse.mealPlan.days.map((d: any, dIdx: number) => ({
          id: `day_${Date.now()}_${dIdx}`,
          day: d.day,
          dayNumber: dIdx + 1,
          totalCalories: (d.meals || []).reduce((acc: number, m: any) => acc + (m.calories || 0), 0),
          totalProtein: (d.meals || []).reduce((acc: number, m: any) => acc + (m.protein || 0), 0),
          totalCarbs: (d.meals || []).reduce((acc: number, m: any) => acc + (m.carbs || 0), 0),
          totalFats: (d.meals || []).reduce((acc: number, m: any) => acc + (m.fats || 0), 0),
          meals: (d.meals || []).map((m: any, mIdx: number) => ({
            id: `meal_${Date.now()}_${dIdx}_${mIdx}`,
            type: m.type,
            name: m.name,
            calories: m.calories,
            protein: m.protein,
            carbs: m.carbs,
            fats: m.fats,
            preparation: m.preparation || '',
            alternatives: m.alternatives || [],
            preparationTime: m.preparationTime || '15 min',
            isCompleted: false,
            orderIndex: mIdx,
            foods: (m.foods || []).map((f: any, fIdx: number) => ({
              id: `food_${Date.now()}_${dIdx}_${mIdx}_${fIdx}`,
              name: f.name,
              quantity: f.quantity,
              calories: f.calories,
              protein: f.protein,
              carbs: f.carbs,
              fats: f.fats
            }))
          }))
        })),
        isActive: true,
        createdAt: new Date().toISOString()
      };

      const savedPlan = db.saveMealPlan(userId, newPlan);
      console.log(`[Generate Meal Plan] ✅ Succès : Programme ${savedPlan.id} | Fournisseur : ${providerName}`);

      // Inclure les métadonnées de génération dans la réponse
      return res.status(201).json({
        ...savedPlan,
        generatedBy: {
          isRealAI,
          providerName,
          usedFallback: !isRealAI
        }
      });
    } catch (err: any) {
      console.error('[Generate Meal Plan] Error:', err);
      return res.status(500).json({ 
        error: `Échec de la génération du programme alimentaire: ${err.message}` 
      });
    }
  });


  /**
   * GET /nutrition/meal-plan/current
   * Récupère le programme actif de l'utilisateur
   */
  router.get('/meal-plan/current', (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const activePlan = db.getActiveMealPlan(userId);
      if (!activePlan) {
        return res.status(404).json({ error: 'Aucun programme actif trouvé.' });
      }
      return res.json(activePlan);
    } catch (err: any) {
      return res.status(500).json({ error: 'Erreur lors de la récupération du programme actif.' });
    }
  });

  /**
   * GET /nutrition/meal-plan/history
   * Récupère l'historique des programmes de l'utilisateur
   */
  router.get('/meal-plan/history', (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const history = db.getMealPlanHistory(userId);
      return res.json(history);
    } catch (err: any) {
      return res.status(500).json({ error: 'Erreur lors de la récupération de l\'historique.' });
    }
  });

  /**
   * GET /nutrition/meal-plan/:id
   */
  /**
   * GET /nutrition/meal-plan/:id
   */
  router.get('/meal-plan/:id', (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const planId = String(req.params['id']);
      const plan = db.getMealPlanById(planId, userId);
      if (!plan) {
        return res.status(404).json({ error: 'Programme non trouvé.' });
      }
      return res.json(plan);
    } catch (err: any) {
      return res.status(500).json({ error: 'Erreur lors de la récupération du programme.' });
    }
  });

  /**
   * POST /nutrition/meal-plan/:id/activate
   */
  router.post('/meal-plan/:id/activate', (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const planId = String(req.params['id']);
      const success = db.setActiveMealPlan(planId, userId);
      if (!success) {
        return res.status(404).json({ error: 'Programme non trouvé.' });
      }
      return res.json({ success: true, message: 'Programme activé avec succès.' });
    } catch (err: any) {
      return res.status(500).json({ error: 'Erreur lors de l\'activation.' });
    }
  });

  /**
   * DELETE /nutrition/meal-plan/:id
   */
  router.delete('/meal-plan/:id', (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const planId = String(req.params['id']);
      db.deleteMealPlan(planId, userId);
      return res.json({ success: true });
    } catch (err: any) {
      return res.status(500).json({ error: 'Erreur lors de la suppression.' });
    }
  });

  /**
   * POST /nutrition/meal/:mealId/regenerate
   * Régénère un seul repas par l'IA
   */
  router.post('/meal/:mealId/regenerate', async (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const profile = db.getProfile(userId);
      if (!profile) return res.status(404).json({ error: 'Profil introuvable.' });

      const mealId = String(req.params['mealId']);
      const existingMeal = db.getMealById(mealId);
      if (!existingMeal) return res.status(404).json({ error: 'Repas introuvable.' });

      const newMealData = await aiService.regenerateMeal(profile, existingMeal);
      const updated = db.updateMeal(mealId, newMealData);
      return res.json(updated);
    } catch (err: any) {
      console.error('[Regenerate Meal] Error:', err);
      return res.status(500).json({ error: `Erreur lors de la régénération du repas : ${err.message}` });
    }
  });

  /**
   * POST /nutrition/meal/:mealId/replace-food
   * Remplace un aliment dans un repas avec l'IA
   */
  router.post('/meal/:mealId/replace-food', async (req: Request, res: Response) => {
    try {
      const userId = getUserId(req);
      const profile = db.getProfile(userId);
      if (!profile) return res.status(404).json({ error: 'Profil introuvable.' });

      const mealId = String(req.params['mealId']);
      const meal = db.getMealById(mealId);
      if (!meal) return res.status(404).json({ error: 'Repas introuvable.' });

      const foodId = req.body.foodId;
      const targetFood = meal.foods.find(f => f.id === foodId) || meal.foods[0];
      if (!targetFood) return res.status(404).json({ error: 'Aliment introuvable dans ce repas.' });

      const replacementFood = await aiService.replaceFood(profile, targetFood, meal);

      // Remplacer l'aliment dans la liste
      const updatedFoods = meal.foods.map(f => f.id === targetFood.id ? { ...replacementFood, id: f.id } : f);
      const totalCalories = updatedFoods.reduce((acc, f) => acc + f.calories, 0);
      const totalProtein = updatedFoods.reduce((acc, f) => acc + f.protein, 0);
      const totalCarbs = updatedFoods.reduce((acc, f) => acc + f.carbs, 0);
      const totalFats = updatedFoods.reduce((acc, f) => acc + f.fats, 0);

      const updated = db.updateMeal(mealId, {
        foods: updatedFoods,
        calories: totalCalories,
        protein: totalProtein,
        carbs: totalCarbs,
        fats: totalFats
      });

      return res.json(updated);
    } catch (err: any) {
      console.error('[Replace Food] Error:', err);
      return res.status(500).json({ error: `Erreur lors du remplacement de l'aliment : ${err.message}` });
    }
  });

  /**
   * PATCH /nutrition/meal/:mealId/toggle-complete
   * Marque le repas comme terminé ou non
   */
  router.patch('/meal/:mealId/toggle-complete', (req: Request, res: Response) => {
    try {
      const mealId = String(req.params['mealId']);
      const isCompleted = Boolean(req.body.isCompleted);
      db.toggleMealCompleted(mealId, isCompleted);
      const updated = db.getMealById(mealId);
      return res.json(updated);
    } catch (err: any) {
      return res.status(500).json({ error: 'Erreur lors du changement de statut.' });
    }
  });

  return router;
}
