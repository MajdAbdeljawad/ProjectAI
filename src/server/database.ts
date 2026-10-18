import { createRequire } from 'node:module';
import type { DatabaseSync as DatabaseSyncType } from 'node:sqlite';
import { mkdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { 
  DailyPlan, 
  FoodItem, 
  Meal, 
  MealPlan, 
  NutritionProfile 
} from '../app/core/models/nutrition.models';

const nodeRequire = createRequire(import.meta.url);
const { DatabaseSync } = nodeRequire('node:sqlite');

export class DatabaseService {
  private db: DatabaseSyncType;
  private static instance: DatabaseService | null = null;

  private constructor() {
    const dataDir = join(process.cwd(), 'data');
    if (!existsSync(dataDir)) {
      mkdirSync(dataDir, { recursive: true });
    }
    const dbPath = join(dataDir, 'nutrition.db');
    this.db = new DatabaseSync(dbPath);
    this.initSchema();
    this.seedDefaultUser();
  }

  public static getInstance(): DatabaseService {
    if (!DatabaseService.instance) {
      DatabaseService.instance = new DatabaseService();
    }
    return DatabaseService.instance;
  }

  private initSchema(): void {
    this.db.exec(`
      PRAGMA foreign_keys = ON;

      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT UNIQUE,
        name TEXT,
        created_at TEXT
      );

      CREATE TABLE IF NOT EXISTS nutrition_profiles (
        id TEXT PRIMARY KEY,
        user_id TEXT UNIQUE,
        full_name TEXT,
        age INTEGER,
        sex TEXT,
        height REAL,
        weight REAL,
        sports_level TEXT,
        sessions_per_week INTEGER,
        workout_type TEXT,
        goal TEXT,
        diet TEXT,
        preferred_foods TEXT,
        disliked_foods TEXT,
        allergies TEXT,
        intolerances TEXT,
        dietary_restrictions TEXT,
        meals_per_day INTEGER,
        wake_up_time TEXT,
        sleep_time TEXT,
        budget TEXT,
        cooking_time TEXT,
        bmr INTEGER,
        tdee INTEGER,
        target_calories INTEGER,
        target_protein INTEGER,
        target_carbs INTEGER,
        target_fats INTEGER,
        created_at TEXT,
        updated_at TEXT,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS meal_plans (
        id TEXT PRIMARY KEY,
        user_id TEXT,
        title TEXT,
        duration_days INTEGER,
        target_calories INTEGER,
        target_protein INTEGER,
        target_carbs INTEGER,
        target_fats INTEGER,
        is_active INTEGER DEFAULT 1,
        created_at TEXT,
        updated_at TEXT,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS daily_plans (
        id TEXT PRIMARY KEY,
        meal_plan_id TEXT,
        day TEXT,
        day_number INTEGER,
        total_calories INTEGER,
        total_protein INTEGER,
        total_carbs INTEGER,
        total_fats INTEGER,
        created_at TEXT,
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plans(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS meals (
        id TEXT PRIMARY KEY,
        daily_plan_id TEXT,
        meal_type TEXT,
        name TEXT,
        calories INTEGER,
        protein REAL,
        carbs REAL,
        fats REAL,
        preparation TEXT,
        preparation_time TEXT,
        alternatives TEXT,
        is_completed INTEGER DEFAULT 0,
        order_index INTEGER DEFAULT 0,
        created_at TEXT,
        FOREIGN KEY (daily_plan_id) REFERENCES daily_plans(id) ON DELETE CASCADE
      );

      CREATE TABLE IF NOT EXISTS foods (
        id TEXT PRIMARY KEY,
        meal_id TEXT,
        name TEXT,
        quantity TEXT,
        calories INTEGER,
        protein REAL,
        carbs REAL,
        fats REAL,
        FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE
      );
    `);
  }

  private seedDefaultUser(): void {
    const defaultUserId = 'default_user_1';
    const userStmt = this.db.prepare('SELECT id FROM users WHERE id = ?');
    const existing = userStmt.get(defaultUserId);

    if (!existing) {
      const insertUser = this.db.prepare(
        'INSERT INTO users (id, email, name, created_at) VALUES (?, ?, ?, ?)'
      );
      insertUser.run(
        defaultUserId,
        'sportif@training.fr',
        'Alexandre Laurent',
        new Date().toISOString()
      );

      // Seed initial default profile
      const insertProfile = this.db.prepare(`
        INSERT INTO nutrition_profiles (
          id, user_id, full_name, age, sex, height, weight,
          sports_level, sessions_per_week, workout_type,
          goal, diet, preferred_foods, disliked_foods, allergies,
          intolerances, dietary_restrictions, meals_per_day,
          wake_up_time, sleep_time, budget, cooking_time,
          bmr, tdee, target_calories, target_protein, target_carbs, target_fats,
          created_at, updated_at
        ) VALUES (
          ?, ?, ?, ?, ?, ?, ?,
          ?, ?, ?,
          ?, ?, ?, ?, ?,
          ?, ?, ?,
          ?, ?, ?, ?,
          ?, ?, ?, ?, ?, ?,
          ?, ?
        )
      `);

      insertProfile.run(
        'prof_default_1',
        defaultUserId,
        'Alexandre Laurent',
        26,
        'homme',
        180,
        78,
        'intermédiaire',
        4,
        'musculation',
        'prise_de_masse',
        'omnivore',
        JSON.stringify(['Poulet', 'Riz basmati', 'Oeufs', 'Avocat', 'Avoine', 'Banane']),
        JSON.stringify(['Chou de Bruxelles', 'Céleri']),
        JSON.stringify([]),
        JSON.stringify(['Lactose léger']),
        JSON.stringify([]),
        4,
        '07:00',
        '23:00',
        'modere',
        'moyen',
        1790,
        2775,
        3125,
        164,
        390,
        82,
        new Date().toISOString(),
        new Date().toISOString()
      );
    }
  }

  public getProfile(userId: string): NutritionProfile | null {
    const stmt = this.db.prepare('SELECT * FROM nutrition_profiles WHERE user_id = ?');
    const row = stmt.get(userId) as any;
    if (!row) return null;

    return {
      id: row.id,
      userId: row.user_id,
      fullName: row.full_name,
      age: Number(row.age),
      sex: row.sex,
      height: Number(row.height),
      weight: Number(row.weight),
      sportsLevel: row.sports_level,
      sessionsPerWeek: Number(row.sessions_per_week),
      workoutType: row.workout_type,
      goal: row.goal,
      diet: row.diet,
      preferredFoods: row.preferred_foods ? JSON.parse(row.preferred_foods) : [],
      dislikedFoods: row.disliked_foods ? JSON.parse(row.disliked_foods) : [],
      allergies: row.allergies ? JSON.parse(row.allergies) : [],
      intolerances: row.intolerances ? JSON.parse(row.intolerances) : [],
      dietaryRestrictions: row.dietary_restrictions ? JSON.parse(row.dietary_restrictions) : [],
      mealsPerDay: Number(row.meals_per_day),
      wakeUpTime: row.wake_up_time,
      sleepTime: row.sleep_time,
      budget: row.budget,
      cookingTime: row.cooking_time,
      bmr: Number(row.bmr),
      tdee: Number(row.tdee),
      targetCalories: Number(row.target_calories),
      targetProtein: Number(row.target_protein),
      targetCarbs: Number(row.target_carbs),
      targetFats: Number(row.target_fats),
      createdAt: row.created_at,
      updatedAt: row.updated_at
    };
  }

  public saveOrUpdateProfile(userId: string, data: Partial<NutritionProfile>): NutritionProfile {
    const existing = this.getProfile(userId);
    const now = new Date().toISOString();

    const id = existing ? existing.id : `prof_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const fullName = data.fullName ?? existing?.fullName ?? 'Utilisateur';

    // S'assurer que l'utilisateur existe dans la table users
    const userStmt = this.db.prepare('SELECT id FROM users WHERE id = ?');
    if (!userStmt.get(userId)) {
      this.db.prepare('INSERT INTO users (id, email, name, created_at) VALUES (?, ?, ?, ?)').run(
        userId,
        `${userId}@app.local`,
        fullName,
        now
      );
    }

    const age = Number(data.age ?? existing?.age ?? 25);
    const sex = data.sex ?? existing?.sex ?? 'homme';
    const height = Number(data.height ?? existing?.height ?? 175);
    const weight = Number(data.weight ?? existing?.weight ?? 70);
    const sportsLevel = data.sportsLevel ?? existing?.sportsLevel ?? 'intermédiaire';
    const sessionsPerWeek = Number(data.sessionsPerWeek ?? existing?.sessionsPerWeek ?? 3);
    const workoutType = data.workoutType ?? existing?.workoutType ?? 'musculation';
    const goal = data.goal ?? existing?.goal ?? 'maintien';
    const diet = data.diet ?? existing?.diet ?? 'omnivore';
    const preferredFoods = JSON.stringify(data.preferredFoods ?? existing?.preferredFoods ?? []);
    const dislikedFoods = JSON.stringify(data.dislikedFoods ?? existing?.dislikedFoods ?? []);
    const allergies = JSON.stringify(data.allergies ?? existing?.allergies ?? []);
    const intolerances = JSON.stringify(data.intolerances ?? existing?.intolerances ?? []);
    const dietaryRestrictions = JSON.stringify(data.dietaryRestrictions ?? existing?.dietaryRestrictions ?? []);
    const mealsPerDay = Number(data.mealsPerDay ?? existing?.mealsPerDay ?? 3);
    const wakeUpTime = data.wakeUpTime ?? existing?.wakeUpTime ?? '07:00';
    const sleepTime = data.sleepTime ?? existing?.sleepTime ?? '23:00';
    const budget = data.budget ?? existing?.budget ?? 'modere';
    const cookingTime = data.cookingTime ?? existing?.cookingTime ?? 'moyen';
    const bmr = Number(data.bmr ?? existing?.bmr ?? 1700);
    const tdee = Number(data.tdee ?? existing?.tdee ?? 2400);
    const targetCalories = Number(data.targetCalories ?? existing?.targetCalories ?? 2400);
    const targetProtein = Number(data.targetProtein ?? existing?.targetProtein ?? 150);
    const targetCarbs = Number(data.targetCarbs ?? existing?.targetCarbs ?? 250);
    const targetFats = Number(data.targetFats ?? existing?.targetFats ?? 70);

    if (existing) {
      const updateStmt = this.db.prepare(`
        UPDATE nutrition_profiles SET
          full_name = ?, age = ?, sex = ?, height = ?, weight = ?,
          sports_level = ?, sessions_per_week = ?, workout_type = ?,
          goal = ?, diet = ?, preferred_foods = ?, disliked_foods = ?,
          allergies = ?, intolerances = ?, dietary_restrictions = ?,
          meals_per_day = ?, wake_up_time = ?, sleep_time = ?,
          budget = ?, cooking_time = ?, bmr = ?, tdee = ?,
          target_calories = ?, target_protein = ?, target_carbs = ?,
          target_fats = ?, updated_at = ?
        WHERE user_id = ?
      `);

      updateStmt.run(
        fullName, age, sex, height, weight,
        sportsLevel, sessionsPerWeek, workoutType,
        goal, diet, preferredFoods, dislikedFoods,
        allergies, intolerances, dietaryRestrictions,
        mealsPerDay, wakeUpTime, sleepTime,
        budget, cookingTime, bmr, tdee,
        targetCalories, targetProtein, targetCarbs,
        targetFats, now, userId
      );
    } else {
      const insertStmt = this.db.prepare(`
        INSERT INTO nutrition_profiles (
          id, user_id, full_name, age, sex, height, weight,
          sports_level, sessions_per_week, workout_type,
          goal, diet, preferred_foods, disliked_foods, allergies,
          intolerances, dietary_restrictions, meals_per_day,
          wake_up_time, sleep_time, budget, cooking_time,
          bmr, tdee, target_calories, target_protein, target_carbs, target_fats,
          created_at, updated_at
        ) VALUES (
          ?, ?, ?, ?, ?, ?, ?,
          ?, ?, ?,
          ?, ?, ?, ?, ?,
          ?, ?, ?,
          ?, ?, ?, ?,
          ?, ?, ?, ?, ?, ?,
          ?, ?
        )
      `);

      insertStmt.run(
        id, userId, fullName, age, sex, height, weight,
        sportsLevel, sessionsPerWeek, workoutType,
        goal, diet, preferredFoods, dislikedFoods,
        allergies, intolerances, dietaryRestrictions,
        mealsPerDay, wakeUpTime, sleepTime,
        budget, cookingTime, bmr, tdee,
        targetCalories, targetProtein, targetCarbs,
        targetFats, now, now
      );
    }

    return this.getProfile(userId)!;
  }

  public saveMealPlan(userId: string, plan: MealPlan): MealPlan {
    const now = new Date().toISOString();
    
    // S'assurer que l'utilisateur existe dans la table users
    const userStmt = this.db.prepare('SELECT id FROM users WHERE id = ?');
    if (!userStmt.get(userId)) {
      this.db.prepare('INSERT INTO users (id, email, name, created_at) VALUES (?, ?, ?, ?)').run(
        userId,
        `${userId}@app.local`,
        'Utilisateur',
        now
      );
    }

    // Set all previous plans for user to inactive
    const deactivateStmt = this.db.prepare(
      'UPDATE meal_plans SET is_active = 0 WHERE user_id = ?'
    );
    deactivateStmt.run(userId);

    const planId = plan.id || `plan_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

    const insertPlan = this.db.prepare(`
      INSERT INTO meal_plans (
        id, user_id, title, duration_days,
        target_calories, target_protein, target_carbs, target_fats,
        is_active, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
    `);

    insertPlan.run(
      planId,
      userId,
      plan.title || 'Programme Nutritionnel 7 Jours',
      plan.durationDays || 7,
      plan.dailyTarget.calories,
      plan.dailyTarget.protein,
      plan.dailyTarget.carbs,
      plan.dailyTarget.fats,
      now,
      now
    );

    const insertDaily = this.db.prepare(`
      INSERT INTO daily_plans (
        id, meal_plan_id, day, day_number,
        total_calories, total_protein, total_carbs, total_fats, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    const insertMeal = this.db.prepare(`
      INSERT INTO meals (
        id, daily_plan_id, meal_type, name,
        calories, protein, carbs, fats,
        preparation, preparation_time, alternatives, is_completed, order_index, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    const insertFood = this.db.prepare(`
      INSERT INTO foods (
        id, meal_id, name, quantity, calories, protein, carbs, fats
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const day of plan.days) {
      const dailyId = day.id || `day_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
      insertDaily.run(
        dailyId,
        planId,
        day.day,
        day.dayNumber,
        day.totalCalories,
        day.totalProtein,
        day.totalCarbs,
        day.totalFats,
        now
      );

      let orderIdx = 0;
      for (const meal of day.meals) {
        const mealId = meal.id || `meal_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
        insertMeal.run(
          mealId,
          dailyId,
          meal.type,
          meal.name,
          meal.calories,
          meal.protein,
          meal.carbs,
          meal.fats,
          meal.preparation,
          meal.preparationTime || '15 min',
          JSON.stringify(meal.alternatives || []),
          meal.isCompleted ? 1 : 0,
          orderIdx++,
          now
        );

        for (const food of meal.foods) {
          const foodId = food.id || `food_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
          insertFood.run(
            foodId,
            mealId,
            food.name,
            food.quantity,
            food.calories,
            food.protein,
            food.carbs,
            food.fats
          );
        }
      }
    }

    return this.getMealPlanById(planId, userId)!;
  }

  public getActiveMealPlan(userId: string): MealPlan | null {
    const stmt = this.db.prepare(
      'SELECT id FROM meal_plans WHERE user_id = ? AND is_active = 1 ORDER BY created_at DESC LIMIT 1'
    );
    const row = stmt.get(userId) as any;
    if (!row) return null;
    return this.getMealPlanById(row.id, userId);
  }

  public getMealPlanHistory(userId: string): { id: string; title: string; createdAt: string; isActive: boolean; targetCalories: number; durationDays: number }[] {
    const stmt = this.db.prepare(
      'SELECT id, title, created_at, is_active, target_calories, duration_days FROM meal_plans WHERE user_id = ? ORDER BY created_at DESC'
    );
    const rows = stmt.all(userId) as any[];
    return rows.map(r => ({
      id: r.id,
      title: r.title,
      createdAt: r.created_at,
      isActive: Boolean(r.is_active),
      targetCalories: Number(r.target_calories),
      durationDays: Number(r.duration_days)
    }));
  }

  public setActiveMealPlan(id: string, userId: string): boolean {
    const plan = this.getMealPlanById(id, userId);
    if (!plan) return false;

    this.db.prepare('UPDATE meal_plans SET is_active = 0 WHERE user_id = ?').run(userId);
    this.db.prepare('UPDATE meal_plans SET is_active = 1, updated_at = ? WHERE id = ? AND user_id = ?').run(
      new Date().toISOString(),
      id,
      userId
    );
    return true;
  }

  public deleteMealPlan(id: string, userId: string): boolean {
    const stmt = this.db.prepare('DELETE FROM meal_plans WHERE id = ? AND user_id = ?');
    stmt.run(id, userId);
    return true;
  }

  public getMealPlanById(id: string, userId: string): MealPlan | null {
    const planStmt = this.db.prepare('SELECT * FROM meal_plans WHERE id = ? AND user_id = ?');
    const planRow = planStmt.get(id, userId) as any;
    if (!planRow) return null;

    const daysStmt = this.db.prepare(
      'SELECT * FROM daily_plans WHERE meal_plan_id = ? ORDER BY day_number ASC'
    );
    const dayRows = daysStmt.all(id) as any[];

    const days: DailyPlan[] = [];

    for (const d of dayRows) {
      const mealsStmt = this.db.prepare(
        'SELECT * FROM meals WHERE daily_plan_id = ? ORDER BY order_index ASC'
      );
      const mealRows = mealsStmt.all(d.id) as any[];

      const meals: Meal[] = [];

      for (const m of mealRows) {
        const foodsStmt = this.db.prepare('SELECT * FROM foods WHERE meal_id = ?');
        const foodRows = foodsStmt.all(m.id) as any[];

        const foods: FoodItem[] = foodRows.map(f => ({
          id: f.id,
          name: f.name,
          quantity: f.quantity,
          calories: Number(f.calories),
          protein: Number(f.protein),
          carbs: Number(f.carbs),
          fats: Number(f.fats)
        }));

        meals.push({
          id: m.id,
          dailyPlanId: d.id,
          type: m.meal_type,
          name: m.name,
          foods,
          calories: Number(m.calories),
          protein: Number(m.protein),
          carbs: Number(m.carbs),
          fats: Number(m.fats),
          preparation: m.preparation || '',
          alternatives: m.alternatives ? JSON.parse(m.alternatives) : [],
          preparationTime: m.preparation_time || '15 min',
          isCompleted: Boolean(m.is_completed),
          orderIndex: Number(m.order_index)
        });
      }

      days.push({
        id: d.id,
        mealPlanId: id,
        day: d.day,
        dayNumber: Number(d.day_number),
        meals,
        totalCalories: Number(d.total_calories),
        totalProtein: Number(d.total_protein),
        totalCarbs: Number(d.total_carbs),
        totalFats: Number(d.total_fats)
      });
    }

    return {
      id: planRow.id,
      userId: planRow.user_id,
      title: planRow.title,
      durationDays: Number(planRow.duration_days),
      dailyTarget: {
        calories: Number(planRow.target_calories),
        protein: Number(planRow.target_protein),
        carbs: Number(planRow.target_carbs),
        fats: Number(planRow.target_fats)
      },
      days,
      isActive: Boolean(planRow.is_active),
      createdAt: planRow.created_at,
      updatedAt: planRow.updated_at
    };
  }

  public getMealById(mealId: string): Meal | null {
    const stmt = this.db.prepare('SELECT * FROM meals WHERE id = ?');
    const m = stmt.get(mealId) as any;
    if (!m) return null;

    const foodsStmt = this.db.prepare('SELECT * FROM foods WHERE meal_id = ?');
    const foodRows = foodsStmt.all(m.id) as any[];

    const foods: FoodItem[] = foodRows.map(f => ({
      id: f.id,
      name: f.name,
      quantity: f.quantity,
      calories: Number(f.calories),
      protein: Number(f.protein),
      carbs: Number(f.carbs),
      fats: Number(f.fats)
    }));

    return {
      id: m.id,
      dailyPlanId: m.daily_plan_id,
      type: m.meal_type,
      name: m.name,
      foods,
      calories: Number(m.calories),
      protein: Number(m.protein),
      carbs: Number(m.carbs),
      fats: Number(m.fats),
      preparation: m.preparation || '',
      alternatives: m.alternatives ? JSON.parse(m.alternatives) : [],
      preparationTime: m.preparation_time || '15 min',
      isCompleted: Boolean(m.is_completed),
      orderIndex: Number(m.order_index)
    };
  }

  public updateMeal(mealId: string, updated: Partial<Meal>): Meal | null {
    const existing = this.getMealById(mealId);
    if (!existing) return null;

    const name = updated.name ?? existing.name;
    const calories = updated.calories ?? existing.calories;
    const protein = updated.protein ?? existing.protein;
    const carbs = updated.carbs ?? existing.carbs;
    const fats = updated.fats ?? existing.fats;
    const preparation = updated.preparation ?? existing.preparation;
    const preparationTime = updated.preparationTime ?? existing.preparationTime;
    const alternatives = JSON.stringify(updated.alternatives ?? existing.alternatives);
    const isCompleted = updated.isCompleted !== undefined ? (updated.isCompleted ? 1 : 0) : (existing.isCompleted ? 1 : 0);

    this.db.prepare(`
      UPDATE meals SET
        name = ?, calories = ?, protein = ?, carbs = ?, fats = ?,
        preparation = ?, preparation_time = ?, alternatives = ?, is_completed = ?
      WHERE id = ?
    `).run(name, calories, protein, carbs, fats, preparation, preparationTime, alternatives, isCompleted, mealId);

    // If new foods are provided
    if (updated.foods) {
      this.db.prepare('DELETE FROM foods WHERE meal_id = ?').run(mealId);
      const insertFood = this.db.prepare(`
        INSERT INTO foods (id, meal_id, name, quantity, calories, protein, carbs, fats)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `);
      for (const f of updated.foods) {
        insertFood.run(
          f.id || `food_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
          mealId,
          f.name,
          f.quantity,
          f.calories,
          f.protein,
          f.carbs,
          f.fats
        );
      }
    }

    return this.getMealById(mealId);
  }

  public toggleMealCompleted(mealId: string, isCompleted: boolean): boolean {
    const stmt = this.db.prepare('UPDATE meals SET is_completed = ? WHERE id = ?');
    stmt.run(isCompleted ? 1 : 0, mealId);
    return true;
  }
}
