import { Injectable } from '@angular/core';
import { 
  DietType, 
  MacroSplit, 
  NutritionGoal, 
  NutritionProfile, 
  Sex, 
  SportsLevel, 
  WorkoutType 
} from '../models/nutrition.models';

export interface CalculationResult {
  bmr: number;
  tdee: number;
  targetCalories: number;
  macros: MacroSplit;
  disclaimer: string;
}

@Injectable({
  providedIn: 'root'
})
export class NutritionCalculatorService {
  public static readonly MEDICAL_DISCLAIMER = 
    'Ces calculs sont des estimations générales basées sur des modèles scientifiques (formule de Mifflin-St Jeor) et les recommandations de nutrition sportive. Ils ne remplacent pas un suivi diététique ou médical personnalisé.';

  /**
   * Calcule le Métabolisme de Base (BMR / MB) avec la formule de Mifflin-St Jeor
   * Homme : (10 × poids en kg) + (6.25 × taille en cm) - (5 × âge) + 5
   * Femme : (10 × poids en kg) + (6.25 × taille en cm) - (5 × âge) - 161
   * Autre : moyenne des deux
   */
  calculateBMR(weightKg: number, heightCm: number, ageYears: number, sex: Sex): number {
    const base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears;
    let bmr = base;

    if (sex === 'homme') {
      bmr = base + 5;
    } else if (sex === 'femme') {
      bmr = base - 161;
    } else {
      bmr = base - 78;
    }

    return Math.round(Math.max(bmr, 800));
  }

  /**
   * Calcule la Dépense Énergétique Journalière Totale (TDEE / DEJ)
   * en fonction du nombre de séances et du type d'activité sportive.
   */
  calculateTDEE(
    bmr: number, 
    sessionsPerWeek: number, 
    sportsLevel: SportsLevel = 'intermédiaire',
    workoutType: WorkoutType = 'musculation'
  ): number {
    let activityMultiplier = 1.2; // Sédentaire de base

    if (sessionsPerWeek <= 1) {
      activityMultiplier = 1.2;
    } else if (sessionsPerWeek <= 3) {
      activityMultiplier = 1.375;
    } else if (sessionsPerWeek <= 5) {
      activityMultiplier = 1.55;
    } else if (sessionsPerWeek <= 6) {
      activityMultiplier = 1.725;
    } else {
      activityMultiplier = 1.9;
    }

    // Ajustement fin selon intensité du sport
    if (workoutType === 'CrossFit' || workoutType === 'running') {
      activityMultiplier += 0.05;
    } else if (workoutType === 'cardio' || workoutType === 'football') {
      activityMultiplier += 0.025;
    }

    // Ajustement selon le niveau d'expérience
    if (sportsLevel === 'avancé') {
      activityMultiplier += 0.03;
    }

    return Math.round(bmr * activityMultiplier);
  }

  /**
   * Calcule l'apport calorique cible en fonction de l'objectif de l'utilisateur
   */
  calculateTargetCalories(tdee: number, goal: NutritionGoal): number {
    switch (goal) {
      case 'perte_de_poids':
        // Déficit modéré durable (-20%, min TDEE - 500)
        return Math.round(Math.max(tdee - 450, 1200));

      case 'prise_de_masse':
        // Surplus modéré propre (+12% à 15%, ~350 kcal)
        return Math.round(tdee + 350);

      case 'performance':
        // Léger surplus énergétique (+150 kcal)
        return Math.round(tdee + 150);

      case 'reequilibrage':
        // Léger déficit / maintien dynamique (-150 kcal)
        return Math.round(tdee - 150);

      case 'maintien':
      default:
        return Math.round(tdee);
    }
  }

  /**
   * Calcule la répartition des macronutriments (Protéines, Glucides, Lipides)
   * selon le poids, l'objectif et le régime spécifique (ex. cétogène).
   */
  calculateMacros(
    targetCalories: number, 
    weightKg: number, 
    goal: NutritionGoal,
    diet: DietType = 'omnivore'
  ): MacroSplit {
    // Cas spécifique : Régime cétogène (très faible en glucides, très riche en lipides)
    if (diet === 'cetogene') {
      const carbs = 25; // max 25g net carbs
      const proteinPerKg = goal === 'prise_de_masse' ? 2.0 : 1.7;
      const protein = Math.round(weightKg * proteinPerKg);
      const caloriesFromProteinAndCarbs = (protein * 4) + (carbs * 4);
      const remainingCalories = Math.max(targetCalories - caloriesFromProteinAndCarbs, 400);
      const fats = Math.round(remainingCalories / 9);

      return {
        calories: targetCalories,
        protein,
        carbs,
        fats
      };
    }

    // Ratios standards recommandés en nutrition sportive
    // Protéines en g par kg de poids de corps
    let proteinPerKg = 1.8;
    if (goal === 'prise_de_masse') {
      proteinPerKg = 2.1;
    } else if (goal === 'perte_de_poids') {
      // Protéines élevées en déficit pour préserver la masse musculaire
      proteinPerKg = 2.0;
    } else if (goal === 'performance') {
      proteinPerKg = 1.9;
    } else {
      proteinPerKg = 1.6;
    }

    const protein = Math.round(weightKg * proteinPerKg);

    // Lipides : ~0.9g à 1.0g par kg de poids (ou 25-30% des calories totales)
    let fatsPerKg = 0.95;
    if (goal === 'perte_de_poids') {
      fatsPerKg = 0.85;
    } else if (goal === 'prise_de_masse') {
      fatsPerKg = 1.05;
    }

    const fats = Math.round(Math.max(weightKg * fatsPerKg, (targetCalories * 0.25) / 9));

    // Glucides : complètent le reste de l'apport énergétique cible
    // 1g protéine = 4 kcal, 1g glucide = 4 kcal, 1g lipide = 9 kcal
    const caloriesUsed = (protein * 4) + (fats * 9);
    const remainingCalories = Math.max(targetCalories - caloriesUsed, 200);
    const carbs = Math.round(remainingCalories / 4);

    return {
      calories: targetCalories,
      protein,
      carbs,
      fats
    };
  }

  /**
   * Calcule le bilan nutritionnel complet à partir d'un profil partiel ou complet
   */
  calculateFullNeeds(profile: Partial<NutritionProfile>): CalculationResult {
    const weight = Number(profile.weight) || 70;
    const height = Number(profile.height) || 175;
    const age = Number(profile.age) || 25;
    const sex = profile.sex || 'homme';
    const sessions = Number(profile.sessionsPerWeek) || 3;
    const sportsLevel = profile.sportsLevel || 'intermédiaire';
    const workoutType = profile.workoutType || 'musculation';
    const goal = profile.goal || 'maintien';
    const diet = profile.diet || 'omnivore';

    const bmr = this.calculateBMR(weight, height, age, sex);
    const tdee = this.calculateTDEE(bmr, sessions, sportsLevel, workoutType);
    const targetCalories = this.calculateTargetCalories(tdee, goal);
    const macros = this.calculateMacros(targetCalories, weight, goal, diet);

    return {
      bmr,
      tdee,
      targetCalories,
      macros,
      disclaimer: NutritionCalculatorService.MEDICAL_DISCLAIMER
    };
  }
}
