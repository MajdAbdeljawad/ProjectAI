import { describe, it, expect, beforeEach } from 'vitest';
import { DatabaseService } from './database';
import { AIService, DynamicNutritionalAIProvider } from './ai.service';
import { NutritionProfile } from '../app/core/models/nutrition.models';

describe('Backend Nutrition & Database & AI Integration', () => {
  let db: DatabaseService;
  let aiService: AIService;

  // Use unique IDs per test run so we start with clean state
  const runId = Date.now();
  const testUserA = `test_user_alpha_${runId}`;
  const testUserB = `test_user_beta_${runId}`;

  const sampleProfileA: Partial<NutritionProfile> = {
    fullName: 'Lucas Dupont',
    age: 24,
    sex: 'homme',
    height: 182,
    weight: 82,
    sportsLevel: 'avancé',
    sessionsPerWeek: 5,
    workoutType: 'musculation',
    goal: 'prise_de_masse',
    diet: 'omnivore',
    preferredFoods: ['Riz basmati', 'Blanc de poulet', 'Oeufs'],
    dislikedFoods: ['Aubergine'],
    allergies: ['arachides'],
    intolerances: [],
    dietaryRestrictions: [],
    mealsPerDay: 4,
    wakeUpTime: '06:30',
    sleepTime: '22:30',
    budget: 'confortable',
    cookingTime: 'moyen',
    bmr: 1850,
    tdee: 2867,
    targetCalories: 3217,
    targetProtein: 172,
    targetCarbs: 400,
    targetFats: 85
  };

  beforeEach(() => {
    db = DatabaseService.getInstance();
    aiService = new AIService();
  });

  describe('1. Persistance et Isolation du Profil en Base de Données', () => {
    it('devrait créer et persister le profil de l\'utilisateur A', () => {
      const saved = db.saveOrUpdateProfile(testUserA, sampleProfileA);
      expect(saved).toBeDefined();
      expect(saved.userId).toBe(testUserA);
      expect(saved.fullName).toBe('Lucas Dupont');
      expect(saved.targetCalories).toBe(3217);

      // Relecture depuis la base
      const fetched = db.getProfile(testUserA);
      expect(fetched).not.toBeNull();
      expect(fetched?.weight).toBe(82);
      expect(fetched?.allergies).toContain('arachides');
    });

    it('devrait modifier le profil (ex: passage à perte de poids) et mettre à jour la base', () => {
      // Modification de l'objectif
      const updated = db.saveOrUpdateProfile(testUserA, {
        goal: 'perte_de_poids',
        weight: 80,
        targetCalories: 2400
      });

      expect(updated.goal).toBe('perte_de_poids');
      expect(updated.weight).toBe(80);
      expect(updated.targetCalories).toBe(2400);

      // Vérification relecture
      const reFetched = db.getProfile(testUserA);
      expect(reFetched?.goal).toBe('perte_de_poids');
      expect(reFetched?.weight).toBe(80);
      expect(reFetched?.targetCalories).toBe(2400);
    });

    it('devrait garantir l\'isolation de sécurité : l\'utilisateur B ne peut pas voir le profil de l\'utilisateur A', () => {
      const profileB = db.getProfile(testUserB);
      // L'utilisateur B n'a pas encore de profil créé
      expect(profileB).toBeNull();

      // Création du profil distinct pour B
      db.saveOrUpdateProfile(testUserB, {
        fullName: 'Sophie Bernard',
        age: 28,
        sex: 'femme',
        height: 165,
        weight: 58,
        goal: 'perte_de_poids',
        targetCalories: 1550,
        allergies: ['gluten']
      });

      const profileA = db.getProfile(testUserA);
      const profileBAfter = db.getProfile(testUserB);

      expect(profileA?.fullName).toBe('Lucas Dupont');
      expect(profileBAfter?.fullName).toBe('Sophie Bernard');
      expect(profileA?.targetCalories).not.toBe(profileBAfter?.targetCalories);
      expect(profileBAfter?.allergies).toContain('gluten');
      expect(profileA?.allergies).not.toContain('gluten');
    });
  });

  describe('2. Prompt Dynamique et Génération IA de Programme 7 Jours', () => {
    it('devrait construire un prompt dynamique exhaustif contenant toutes les informations du profil', () => {
      const profile = db.getProfile(testUserA)!;
      const prompt = aiService.buildPrompt(profile);

      expect(prompt).toContain('Lucas Dupont');
      expect(prompt).toContain('80 kg');
      expect(prompt).toContain('arachides');
      expect(prompt).toContain('Aubergine');
      expect(prompt).toContain('2400 kcal');
      expect(prompt).toContain('7 jours');
      expect(prompt).toContain('Lundi');
      expect(prompt).toContain('Dimanche');
    });

    it('devrait générer un programme complet de 7 jours adapté au profil', async () => {
      const profile = db.getProfile(testUserA)!;
      const result = await aiService.generatePlan(profile);

      expect(result.mealPlan).toBeDefined();
      expect(result.mealPlan.durationDays).toBe(7);
      expect(result.mealPlan.days.length).toBe(7);

      // Vérifier chaque jour
      for (const day of result.mealPlan.days) {
        expect(['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche']).toContain(day.day);
        expect(day.meals.length).toBeGreaterThanOrEqual(2);
        for (const meal of day.meals) {
          expect(meal.name).toBeTruthy();
          expect(meal.calories).toBeGreaterThan(0);
          expect(meal.protein).toBeGreaterThan(0);
          expect(meal.foods.length).toBeGreaterThan(0);
        }
      }
    });
  });

  describe('3. Validation Stricte des Réponses de l\'IA et Sécurité Allergènes', () => {
    it('devrait rejeter une réponse IA contenant un allergène déclaré par le sportif', () => {
      const profile = db.getProfile(testUserA)!; // Allergie déclarée : "arachides"

      const poisonedAIResponse = {
        mealPlan: {
          durationDays: 7,
          dailyTarget: { calories: 2400, protein: 150, carbs: 250, fats: 70 },
          days: Array.from({ length: 7 }, (_, i) => ({
            day: ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'][i],
            meals: [
              {
                type: 'breakfast',
                name: 'Tartine au beurre d\'arachides et banane', // VIOLATION ALLERGÈNE !
                calories: 500,
                protein: 20,
                carbs: 50,
                fats: 20,
                foods: [{ name: 'Beurre d\'arachides', quantity: '30g', calories: 200, protein: 8, carbs: 6, fats: 16 }]
              }
            ]
          }))
        }
      };

      const validation = aiService.validateAIResponse(poisonedAIResponse, profile);
      expect(validation.valid).toBe(false);
      expect(validation.errors.some(e => e.includes('Violation d\'allergène'))).toBe(true);
    });

    it('devrait rejeter une réponse IA incomplète ou corrompue', () => {
      const profile = db.getProfile(testUserA)!;
      const invalidResponse = {
        mealPlan: {
          durationDays: 3, // Incomplet, attendu 7
          days: []
        }
      };

      const validation = aiService.validateAIResponse(invalidResponse, profile);
      expect(validation.valid).toBe(false);
      expect(validation.errors.length).toBeGreaterThan(0);
    });
  });

  describe('4. Sauvegarde et Persistance du Programme Alimentaire en Base', () => {
    it('devrait enregistrer le programme généré en base et permettre sa réactivation', async () => {
      const profile = db.getProfile(testUserA)!;
      const generated = await aiService.generatePlan(profile);

      const ts = Date.now();
      const planPayload = {
        id: `plan_test_${ts}`,
        userId: testUserA,
        title: 'Programme Test Découpe',
        durationDays: 7,
        dailyTarget: generated.mealPlan.dailyTarget,
        isActive: true,
        createdAt: new Date().toISOString(),
        days: generated.mealPlan.days.map((d, dIdx) => ({
          id: `day_test_${ts}_${dIdx}`,
          day: d.day,
          dayNumber: dIdx + 1,
          totalCalories: d.meals.reduce((s, m) => s + m.calories, 0),
          totalProtein: d.meals.reduce((s, m) => s + m.protein, 0),
          totalCarbs: d.meals.reduce((s, m) => s + m.carbs, 0),
          totalFats: d.meals.reduce((s, m) => s + m.fats, 0),
          meals: d.meals.map((m, mIdx) => ({
            id: `meal_test_${ts}_${dIdx}_${mIdx}`,
            type: m.type,
            name: m.name,
            calories: m.calories,
            protein: m.protein,
            carbs: m.carbs,
            fats: m.fats,
            preparation: m.preparation,
            alternatives: m.alternatives,
            preparationTime: m.preparationTime,
            isCompleted: false,
            orderIndex: mIdx,
            foods: m.foods.map((f, fIdx) => ({
              id: `food_test_${ts}_${dIdx}_${mIdx}_${fIdx}`,
              name: f.name,
              quantity: f.quantity,
              calories: f.calories,
              protein: f.protein,
              carbs: f.carbs,
              fats: f.fats
            }))
          }))
        }))
      };

      const saved = db.saveMealPlan(testUserA, planPayload);
      expect(saved.id).toBe(planPayload.id);

      // Vérifier que le programme actif est bien celui-ci
      const active = db.getActiveMealPlan(testUserA);
      expect(active).not.toBeNull();
      expect(active?.id).toBe(planPayload.id);
      expect(active?.days.length).toBe(7);

      // Tester le basculement "Repas terminé"
      const firstMeal = active!.days[0].meals[0];
      expect(firstMeal.isCompleted).toBe(false);

      db.toggleMealCompleted(firstMeal.id, true);
      const updatedMeal = db.getMealById(firstMeal.id);
      expect(updatedMeal?.isCompleted).toBe(true);
    });
  });

  describe('5. Régénération d\'un Repas et Remplacement d\'Aliment par l\'IA', () => {
    it('devrait régénérer un repas unique avec de nouvelles valeurs et respecter les macros', async () => {
      const profile = db.getProfile(testUserA)!;
      const initialMeal = {
        id: 'meal_regen_test',
        dailyPlanId: 'day_1',
        type: 'lunch' as const,
        name: 'Ancien repas poulet',
        calories: 650,
        protein: 45,
        carbs: 70,
        fats: 18,
        preparation: '',
        alternatives: [],
        preparationTime: '20 min',
        isCompleted: false,
        orderIndex: 1,
        foods: []
      };

      const regenerated = await aiService.regenerateMeal(profile, initialMeal);
      expect(regenerated).toBeDefined();
      expect(regenerated.type).toBe('lunch');
      expect(regenerated.calories).toBeGreaterThan(0);
      expect(regenerated.foods.length).toBeGreaterThan(0);
    });

    it('devrait remplacer un aliment par un substitut équivalent', async () => {
      const profile = db.getProfile(testUserA)!;
      const meal = {
        id: 'meal_rep_test',
        type: 'lunch' as const,
        name: 'Plat test',
        calories: 600,
        protein: 40,
        carbs: 60,
        fats: 15,
        preparation: '',
        alternatives: [],
        preparationTime: '15 min',
        isCompleted: false,
        orderIndex: 0,
        foods: []
      };

      const foodToReplace = {
        id: 'f_test_1',
        name: 'Blanc de poulet émincé',
        quantity: '150g',
        calories: 165,
        protein: 31,
        carbs: 0,
        fats: 3.6
      };

      const replacement = await aiService.replaceFood(profile, foodToReplace, meal);
      expect(replacement).toBeDefined();
      expect(replacement.name).not.toBe(foodToReplace.name);
      expect(replacement.protein).toBeGreaterThan(0);
    });
  });
});
