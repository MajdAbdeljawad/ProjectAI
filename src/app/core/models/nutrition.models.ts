export type Sex = 'homme' | 'femme' | 'autre';

export type SportsLevel = 'débutant' | 'intermédiaire' | 'avancé';

export type WorkoutType = 
  | 'musculation'
  | 'cardio'
  | 'running'
  | 'football'
  | 'CrossFit'
  | 'autre';

export type NutritionGoal = 
  | 'perte_de_poids'
  | 'prise_de_masse'
  | 'maintien'
  | 'performance'
  | 'reequilibrage';

export type DietType = 
  | 'omnivore'
  | 'vegetarien'
  | 'vegan'
  | 'pesco_vegetarien'
  | 'cetogene'
  | 'sans_gluten'
  | 'sans_lactose';

export type BudgetLevel = 'economique' | 'modere' | 'confortable' | 'eleve';

export type CookingTimePreference = 'rapide' | 'moyen' | 'long';

export type MealType = 
  | 'breakfast'
  | 'morning_snack'
  | 'lunch'
  | 'afternoon_snack'
  | 'dinner'
  | 'evening_snack';

export interface MacroSplit {
  calories: number;
  protein: number; // in grams
  carbs: number;   // in grams
  fats: number;    // in grams
}

export interface NutritionProfile {
  id: string;
  userId: string;
  // Informations personnelles
  fullName: string;
  age: number;
  sex: Sex;
  height: number; // cm
  weight: number; // kg
  
  // Informations sportives
  sportsLevel: SportsLevel;
  sessionsPerWeek: number; // 1 to 7
  workoutType: WorkoutType;
  
  // Objectif
  goal: NutritionGoal;
  
  // Préférences alimentaires
  diet: DietType;
  preferredFoods: string[];
  dislikedFoods: string[];
  allergies: string[];
  intolerances: string[];
  dietaryRestrictions: string[];
  
  // Mode de vie
  mealsPerDay: number; // 2 to 6
  wakeUpTime: string; // e.g. "07:00"
  sleepTime: string;  // e.g. "23:00"
  budget: BudgetLevel;
  cookingTime: CookingTimePreference;
  
  // Calculs nutritionnels (estimations)
  bmr: number;
  tdee: number;
  targetCalories: number;
  targetProtein: number;
  targetCarbs: number;
  targetFats: number;
  
  createdAt?: string;
  updatedAt?: string;
}

export interface FoodItem {
  id: string;
  name: string;
  quantity: string;
  calories: number;
  protein: number;
  carbs: number;
  fats: number;
}

export interface Meal {
  id: string;
  dailyPlanId?: string;
  type: MealType;
  name: string;
  foods: FoodItem[];
  calories: number;
  protein: number;
  carbs: number;
  fats: number;
  preparation: string;
  alternatives: string[];
  preparationTime: string;
  isCompleted: boolean;
  orderIndex: number;
}

export interface DailyPlan {
  id: string;
  mealPlanId?: string;
  day: string; // e.g. "Lundi", "Mardi"
  dayNumber: number; // 1 to 7
  meals: Meal[];
  totalCalories: number;
  totalProtein: number;
  totalCarbs: number;
  totalFats: number;
}

export interface MealPlan {
  id: string;
  userId: string;
  title: string;
  durationDays: number;
  dailyTarget: MacroSplit;
  days: DailyPlan[];
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface AIStructuredMealPlanResponse {
  mealPlan: {
    durationDays: number;
    dailyTarget: MacroSplit;
    days: {
      day: string;
      meals: {
        type: MealType;
        name: string;
        foods: {
          name: string;
          quantity: string;
          calories: number;
          protein: number;
          carbs: number;
          fats: number;
        }[];
        calories: number;
        protein: number;
        carbs: number;
        fats: number;
        preparation: string;
        alternatives: string[];
        preparationTime: string;
      }[];
    }[];
  };
}

export interface AIGenerationStep {
  step: number;
  totalSteps: number;
  message: string;
  completed: boolean;
}
