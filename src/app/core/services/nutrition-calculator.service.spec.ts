import { TestBed } from '@angular/core/testing';
import { NutritionCalculatorService } from './nutrition-calculator.service';
import { NutritionProfile } from '../models/nutrition.models';

describe('NutritionCalculatorService', () => {
  let service: NutritionCalculatorService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NutritionCalculatorService]
    });
    service = TestBed.inject(NutritionCalculatorService);
  });

  describe('Calculs du Métabolisme de Base (BMR)', () => {
    it('devrait calculer le BMR exact pour un homme avec Mifflin-St Jeor', () => {
      // Formule Homme : 10 * 80 + 6.25 * 180 - 5 * 25 + 5 = 800 + 1125 - 125 + 5 = 1805
      const bmr = service.calculateBMR(80, 180, 25, 'homme');
      expect(bmr).toBe(1805);
    });

    it('devrait calculer le BMR exact pour une femme avec Mifflin-St Jeor', () => {
      // Formule Femme : 10 * 60 + 6.25 * 165 - 5 * 30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25 -> 1320
      const bmr = service.calculateBMR(60, 165, 30, 'femme');
      expect(bmr).toBe(1320);
    });
  });

  describe('Calculs de la Dépense Énergétique Totale (TDEE)', () => {
    it('devrait appliquer le bon multiplicateur d\'activité selon les séances', () => {
      const bmr = 1800;
      // 1 séance = 1.2
      expect(service.calculateTDEE(bmr, 1, 'débutant', 'cardio')).toBeGreaterThan(2100);
      // 4 séances musculation = 1.55
      const tdeeMuscu = service.calculateTDEE(bmr, 4, 'intermédiaire', 'musculation');
      expect(tdeeMuscu).toBe(Math.round(1800 * 1.55));
      // 6 séances CrossFit = 1.725 + 0.05
      const tdeeCrossfit = service.calculateTDEE(bmr, 6, 'intermédiaire', 'CrossFit');
      expect(tdeeCrossfit).toBeGreaterThan(tdeeMuscu);
    });
  });

  describe('Calculs des Calories Cibles selon l\'Objectif', () => {
    const tdee = 2500;

    it('devrait créer un déficit contrôlé pour la perte de poids', () => {
      const cals = service.calculateTargetCalories(tdee, 'perte_de_poids');
      expect(cals).toBe(2050); // 2500 - 450
      expect(cals).toBeLessThan(tdee);
    });

    it('devrait créer un surplus énergétique propre pour la prise de masse', () => {
      const cals = service.calculateTargetCalories(tdee, 'prise_de_masse');
      expect(cals).toBe(2850); // 2500 + 350
      expect(cals).toBeGreaterThan(tdee);
    });

    it('devrait maintenir l\'équilibre pour le maintien', () => {
      const cals = service.calculateTargetCalories(tdee, 'maintien');
      expect(cals).toBe(2500);
    });
  });

  describe('Calculs de la Répartition des Macronutriments', () => {
    it('devrait calculer des protéines élevées pour la prise de masse et préserver la masse musculaire', () => {
      const weight = 80;
      const targetCals = 2800;
      const macros = service.calculateMacros(targetCals, weight, 'prise_de_masse', 'omnivore');

      expect(macros.protein).toBeGreaterThanOrEqual(160); // au moins 2g/kg
      expect(macros.fats).toBeGreaterThan(50);
      expect(macros.carbs).toBeGreaterThan(150);
      expect(macros.calories).toBe(targetCals);
    });

    it('devrait adapter les macros pour le régime cétogène', () => {
      const weight = 75;
      const targetCals = 2200;
      const macros = service.calculateMacros(targetCals, weight, 'perte_de_poids', 'cetogene');

      expect(macros.carbs).toBeLessThanOrEqual(30); // Max 30g de glucides en cétogène
      expect(macros.fats).toBeGreaterThan(120); // Riche en lipides
    });
  });

  describe('Calcul Complet Intégré', () => {
    it('devrait retourner un bilan complet avec métabolisme, calories, macros et disclaimer médical', () => {
      const profile: Partial<NutritionProfile> = {
        age: 26,
        sex: 'homme',
        height: 180,
        weight: 78,
        sportsLevel: 'intermédiaire',
        sessionsPerWeek: 4,
        workoutType: 'musculation',
        goal: 'prise_de_masse',
        diet: 'omnivore'
      };

      const result = service.calculateFullNeeds(profile);

      expect(result.bmr).toBeGreaterThan(1600);
      expect(result.tdee).toBeGreaterThan(result.bmr);
      expect(result.targetCalories).toBeGreaterThan(result.tdee);
      expect(result.macros.protein).toBeGreaterThan(150);
      expect(result.disclaimer).toContain('Mifflin-St Jeor');
      expect(result.disclaimer).toContain('médical');
    });
  });
});
