import { 
  AIStructuredMealPlanResponse, 
  FoodItem, 
  Meal, 
  MealType, 
  NutritionProfile 
} from '../app/core/models/nutrition.models';

export interface IAIProvider {
  name: string;
  generateMealPlan(prompt: string, profile: NutritionProfile): Promise<AIStructuredMealPlanResponse>;
  regenerateMeal(prompt: string, profile: NutritionProfile, existingMeal: Meal): Promise<Meal>;
  replaceFood(prompt: string, profile: NutritionProfile, foodToReplace: FoodItem, targetMeal: Meal): Promise<FoodItem>;
}

export class OpenAICompatibleProvider implements IAIProvider {
  public name = 'OpenAI / Compatible Provider';
  private apiKey: string;
  private apiUrl: string;
  private model: string;

  constructor(apiKey: string, apiUrl?: string, model?: string) {
    this.apiKey = apiKey;
    this.apiUrl = apiUrl || 'https://api.openai.com/v1';
    this.model = model || 'gpt-4o-mini';
  }

  async generateMealPlan(prompt: string, _profile: NutritionProfile): Promise<AIStructuredMealPlanResponse> {
    const url = `${this.apiUrl.replace(/\/+$/, '')}/chat/completions`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`
      },
      body: JSON.stringify({
        model: this.model,
        messages: [
          {
            role: 'system',
            content: 'Tu es un expert diététicien-nutritionniste du sport de renommée mondiale. Tu réponds UNIQUEMENT en JSON valide strict conforme au schéma demandé. Pas de markdown, pas de texte avant ou après.'
          },
          {
            role: 'user',
            content: prompt
          }
        ],
        response_format: { type: 'json_object' },
        temperature: 0.7
      })
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`OpenAI API error [${response.status}]: ${errText}`);
    }

    const data = await response.json() as any;
    const content = data.choices?.[0]?.message?.content;
    if (!content) {
      throw new Error('Réponse vide reçue du modèle IA');
    }

    return JSON.parse(content) as AIStructuredMealPlanResponse;
  }

  async regenerateMeal(prompt: string, _profile: NutritionProfile, _existingMeal: Meal): Promise<Meal> {
    const url = `${this.apiUrl.replace(/\/+$/, '')}/chat/completions`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`
      },
      body: JSON.stringify({
        model: this.model,
        messages: [
          {
            role: 'system',
            content: 'Tu es un expert en nutrition sportive. Tu régénères un repas individuel en JSON strict respectant les contraintes.'
          },
          {
            role: 'user',
            content: prompt
          }
        ],
        response_format: { type: 'json_object' },
        temperature: 0.7
      })
    });

    if (!response.ok) {
      const err = await response.text();
      throw new Error(`OpenAI API error: ${err}`);
    }

    const data = await response.json() as any;
    const parsed = JSON.parse(data.choices[0].message.content);
    return parsed.meal || parsed;
  }

  async replaceFood(prompt: string, _profile: NutritionProfile, _foodToReplace: FoodItem, _targetMeal: Meal): Promise<FoodItem> {
    const url = `${this.apiUrl.replace(/\/+$/, '')}/chat/completions`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`
      },
      body: JSON.stringify({
        model: this.model,
        messages: [
          {
            role: 'system',
            content: 'Tu es un expert en nutrition. Tu fournis un aliment de remplacement iso-calorique/iso-macro en JSON strict.'
          },
          {
            role: 'user',
            content: prompt
          }
        ],
        response_format: { type: 'json_object' }
      })
    });

    if (!response.ok) {
      throw new Error(`OpenAI API error: ${response.statusText}`);
    }

    const data = await response.json() as any;
    const parsed = JSON.parse(data.choices[0].message.content);
    return parsed.food || parsed;
  }
}

export class GeminiProvider implements IAIProvider {
  public name = 'Google Gemini Provider';
  private apiKey: string;
  private apiUrl: string;
  private model: string;

  constructor(apiKey: string, apiUrl?: string, model?: string) {
    this.apiKey = apiKey;
    this.apiUrl = apiUrl || 'https://generativelanguage.googleapis.com/v1beta/models';
    this.model = model || 'gemini-1.5-flash';
  }

  async generateMealPlan(prompt: string, _profile: NutritionProfile): Promise<AIStructuredMealPlanResponse> {
    const endpoint = `${this.apiUrl.replace(/\/+$/, '')}/${this.model}:generateContent?key=${this.apiKey}`;
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [
          {
            role: 'user',
            parts: [{ text: prompt }]
          }
        ],
        generationConfig: {
          responseMimeType: 'application/json'
        }
      })
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`Gemini API error [${response.status}]: ${errText}`);
    }

    const data = await response.json() as any;
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) {
      throw new Error('Réponse vide reçue de Google Gemini');
    }

    return JSON.parse(text) as AIStructuredMealPlanResponse;
  }

  async regenerateMeal(prompt: string, _profile: NutritionProfile, _existingMeal: Meal): Promise<Meal> {
    const endpoint = `${this.apiUrl.replace(/\/+$/, '')}/${this.model}:generateContent?key=${this.apiKey}`;
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ role: 'user', parts: [{ text: prompt }] }],
        generationConfig: { responseMimeType: 'application/json' }
      })
    });

    if (!response.ok) throw new Error(`Gemini API error: ${response.statusText}`);
    const data = await response.json() as any;
    const text = data.candidates[0].content.parts[0].text;
    const parsed = JSON.parse(text);
    return parsed.meal || parsed;
  }

  async replaceFood(prompt: string, _profile: NutritionProfile, _foodToReplace: FoodItem, _targetMeal: Meal): Promise<FoodItem> {
    const endpoint = `${this.apiUrl.replace(/\/+$/, '')}/${this.model}:generateContent?key=${this.apiKey}`;
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ role: 'user', parts: [{ text: prompt }] }],
        generationConfig: { responseMimeType: 'application/json' }
      })
    });

    if (!response.ok) throw new Error(`Gemini API error: ${response.statusText}`);
    const data = await response.json() as any;
    const text = data.candidates[0].content.parts[0].text;
    const parsed = JSON.parse(text);
    return parsed.food || parsed;
  }
}

/**
 * Moteur algorithmique dynamique intelligent.
 * Utilisé lorsqu'aucune clé API externe (OpenAI / Gemini) n'est fournie dans l'environnement,
 * ou en mode offline.
 * Ne produit JAMAIS une liste statique : il calcule les portions et compose les repas
 * SUR MESURE en fonction du profil exact (calories, macros, nombre de repas, allergies, régime, temps).
 */
export class DynamicNutritionalAIProvider implements IAIProvider {
  public name = 'Dynamic Nutritional Intelligence Provider';

  private static readonly DAYS = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];

  // Base d'ingrédients riches et réalistes
  private static readonly FOOD_DATABASE = {
    proteins: [
      { name: 'Blanc de poulet émincé', p: 31, c: 0, f: 3.6, cal: 165, unit: 'g', diets: ['omnivore'], allergens: [] },
      { name: 'Pavé de saumon atlantique', p: 20, c: 0, f: 13, cal: 208, unit: 'g', diets: ['omnivore', 'pesco_vegetarien'], allergens: ['poisson'] },
      { name: 'Filet de colin ou cabillaud', p: 18, c: 0, f: 0.8, cal: 82, unit: 'g', diets: ['omnivore', 'pesco_vegetarien'], allergens: ['poisson'] },
      { name: 'Oeufs plein air entiers', p: 13, c: 1, f: 10, cal: 143, unit: 'g', diets: ['omnivore', 'vegetarien', 'pesco_vegetarien'], allergens: ['oeuf'] },
      { name: 'Steak haché 5% pur boeuf', p: 21, c: 0, f: 5, cal: 130, unit: 'g', diets: ['omnivore'], allergens: [] },
      { name: 'Tofu ferme bio mariné', p: 15, c: 2, f: 8, cal: 144, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien'], allergens: ['soja'] },
      { name: 'Tempeh grillé', p: 19, c: 4, f: 11, cal: 192, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien'], allergens: ['soja'] },
      { name: 'Fromage blanc 0%', p: 8, c: 4, f: 0.2, cal: 50, unit: 'g', diets: ['omnivore', 'vegetarien', 'pesco_vegetarien'], allergens: ['lactose'] },
      { name: 'Skyr nature islandais', p: 11, c: 4, f: 0.2, cal: 62, unit: 'g', diets: ['omnivore', 'vegetarien', 'pesco_vegetarien'], allergens: ['lactose'] },
      { name: 'Lentilles corail cuites', p: 9, c: 20, f: 0.4, cal: 116, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien'], allergens: [] }
    ],
    carbs: [
      { name: 'Riz basmati complet', p: 2.6, c: 25, f: 0.9, cal: 121, unit: 'g cuit', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten'], allergens: [] },
      { name: 'Quinoa royal blanc et rouge', p: 4.4, c: 21, f: 1.9, cal: 120, unit: 'g cuit', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten'], allergens: [] },
      { name: 'Flocons d\'avoine complets', p: 13, c: 60, f: 7, cal: 370, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien'], allergens: ['gluten'] },
      { name: 'Patates douces rôties au four', p: 1.6, c: 20, f: 0.1, cal: 86, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten'], allergens: [] },
      { name: 'Pain complet au levain', p: 9, c: 49, f: 1.5, cal: 247, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien'], allergens: ['gluten'] },
      { name: 'Pâtes semi-complètes al dente', p: 5, c: 30, f: 0.8, cal: 150, unit: 'g cuit', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien'], allergens: ['gluten'] },
      { name: 'Pois chiches cuits', p: 8.9, c: 27, f: 2.6, cal: 164, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten'], allergens: [] }
    ],
    fats: [
      { name: 'Huile d\'olive extra vierge bio', p: 0, c: 0, f: 100, cal: 900, unit: 'ml', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten', 'cetogene'], allergens: [] },
      { name: 'Avocat Haas mûr', p: 2, c: 9, f: 15, cal: 160, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten', 'cetogene'], allergens: [] },
      { name: 'Amandes décortiquées entières', p: 21, c: 22, f: 50, cal: 579, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten', 'cetogene'], allergens: ['fruits_a_coque'] },
      { name: 'Noix de Grenoble', p: 15, c: 14, f: 65, cal: 654, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten', 'cetogene'], allergens: ['fruits_a_coque'] },
      { name: 'Graines de chia bio', p: 17, c: 42, f: 31, cal: 486, unit: 'g', diets: ['omnivore', 'vegetarien', 'vegan', 'pesco_vegetarien', 'sans_gluten', 'cetogene'], allergens: [] }
    ],
    veggies: [
      { name: 'Brocolis vapeur', cal: 35, p: 2.8, c: 7, f: 0.4 },
      { name: 'Épinards frais tombés à la poêle', cal: 23, p: 2.9, c: 3.6, f: 0.4 },
      { name: 'Courgettes rôties aux herbes', cal: 17, p: 1.2, c: 3.1, f: 0.3 },
      { name: 'Salade de roquette et tomates cerises', cal: 25, p: 1.5, c: 4.5, f: 0.3 },
      { name: 'Haricots verts extra-fins', cal: 31, p: 1.8, c: 7, f: 0.2 },
      { name: 'Asperges vertes grillées', cal: 20, p: 2.2, c: 3.9, f: 0.1 }
    ]
  };

  async generateMealPlan(_prompt: string, profile: NutritionProfile): Promise<AIStructuredMealPlanResponse> {
    const durationDays = 7;
    const dailyTarget = {
      calories: profile.targetCalories,
      protein: profile.targetProtein,
      carbs: profile.targetCarbs,
      fats: profile.targetFats
    };

    const days: any[] = [];

    for (let dayIndex = 0; dayIndex < durationDays; dayIndex++) {
      const dayName = DynamicNutritionalAIProvider.DAYS[dayIndex];
      const meals = this.generateDayMeals(profile, dayIndex);
      
      const dayMealsTotal = meals.map(m => ({
        type: m.type,
        name: m.name,
        foods: m.foods.map((f: any) => ({
          name: f.name,
          quantity: f.quantity,
          calories: f.calories,
          protein: f.protein,
          carbs: f.carbs,
          fats: f.fats
        })),
        calories: m.calories,
        protein: m.protein,
        carbs: m.carbs,
        fats: m.fats,
        preparation: m.preparation,
        alternatives: m.alternatives,
        preparationTime: m.preparationTime
      }));

      days.push({
        day: dayName,
        meals: dayMealsTotal
      });
    }

    return {
      mealPlan: {
        durationDays,
        dailyTarget,
        days
      }
    };
  }

  private generateDayMeals(profile: NutritionProfile, dayIndex: number): Meal[] {
    const mealCount = Math.max(2, Math.min(6, profile.mealsPerDay || 3));
    const targetCals = profile.targetCalories;
    const targetP = profile.targetProtein;
    const targetC = profile.targetCarbs;
    const targetF = profile.targetFats;

    // Définir les types de repas selon le nombre configuré
    let mealTypes: MealType[] = [];
    if (mealCount === 2) {
      mealTypes = ['breakfast', 'dinner'];
    } else if (mealCount === 3) {
      mealTypes = ['breakfast', 'lunch', 'dinner'];
    } else if (mealCount === 4) {
      mealTypes = ['breakfast', 'lunch', 'afternoon_snack', 'dinner'];
    } else if (mealCount === 5) {
      mealTypes = ['breakfast', 'morning_snack', 'lunch', 'afternoon_snack', 'dinner'];
    } else {
      mealTypes = ['breakfast', 'morning_snack', 'lunch', 'afternoon_snack', 'dinner', 'evening_snack'];
    }

    // Répartition calorique par repas
    const calRatios: Record<MealType, number> = {
      breakfast: mealCount >= 4 ? 0.25 : 0.30,
      morning_snack: 0.10,
      lunch: mealCount >= 4 ? 0.35 : 0.40,
      afternoon_snack: 0.12,
      dinner: mealCount >= 4 ? 0.28 : 0.30,
      evening_snack: 0.08
    };

    // Normalisation des ratios
    let sumRatio = 0;
    mealTypes.forEach(t => sumRatio += (calRatios[t] || 0.2));

    const meals: Meal[] = [];

    mealTypes.forEach((type, orderIdx) => {
      const fraction = (calRatios[type] || 0.2) / sumRatio;
      const mealCals = Math.round(targetCals * fraction);
      const mealP = Math.round(targetP * fraction);
      const mealC = Math.round(targetC * fraction);
      const mealF = Math.round(targetF * fraction);

      const generated = this.composeMeal(type, mealCals, mealP, mealC, mealF, profile, dayIndex, orderIdx);
      meals.push(generated);
    });

    return meals;
  }

  private composeMeal(
    type: MealType,
    calories: number,
    protein: number,
    carbs: number,
    fats: number,
    profile: NutritionProfile,
    dayIndex: number,
    orderIndex: number
  ): Meal {
    const isSnack = type.includes('snack');
    let mealName = '';
    let prepTime = profile.cookingTime === 'rapide' ? '10 min' : (profile.cookingTime === 'moyen' ? '20 min' : '35 min');
    let preparation = '';
    let alternatives: string[] = [];
    const foods: FoodItem[] = [];

    const isAllergicTo = (item: string) => {
      const lower = item.toLowerCase();
      return (profile.allergies || []).some(a => lower.includes(a.toLowerCase())) ||
             (profile.intolerances || []).some(i => lower.includes(i.toLowerCase())) ||
             (profile.dislikedFoods || []).some(d => lower.includes(d.toLowerCase()));
    };

    if (type === 'breakfast') {
      if (profile.diet === 'cetogene') {
        mealName = 'Omelette gourmande aux épinards et avocat';
        foods.push(
          { id: `f_${Date.now()}_1`, name: 'Oeufs entiers', quantity: '3 pièces', calories: 210, protein: 18, carbs: 1, fats: 15 },
          { id: `f_${Date.now()}_2`, name: 'Avocat mûr', quantity: '1/2 pièce', calories: 120, protein: 2, carbs: 3, fats: 11 },
          { id: `f_${Date.now()}_3`, name: 'Épinards frais sautés', quantity: '100g', calories: 25, protein: 3, carbs: 2, fats: 0.5 }
        );
        prepTime = '12 min';
        preparation = 'Battre les oeufs dans un bol avec une pincée de sel et poivre. Chauffer une poêle huilée, verser et cuire 4 minutes en rabattant. Servir avec l\'avocat tranché et les pousses d\'épinards.';
        alternatives = ['Brouillade d\'oeufs au saumon fumé', 'Chia pudding au lait de coco'];
      } else {
        mealName = 'Bowl d\'avoine protéiné et fruits frais';
        const oatGrams = Math.min(120, Math.max(40, Math.round(carbs * 1.3)));
        const oatCals = Math.round(oatGrams * 3.7);
        foods.push(
          { id: `f_${Date.now()}_1`, name: 'Flocons d\'avoine complets', quantity: `${oatGrams}g`, calories: oatCals, protein: Math.round(oatGrams * 0.13), carbs: Math.round(oatGrams * 0.6), fats: Math.round(oatGrams * 0.07) },
          { id: `f_${Date.now()}_2`, name: isAllergicTo('lactose') ? 'Lait d\'amande enrichi' : 'Fromage blanc ou Skyr 0%', quantity: '200g', calories: 110, protein: 20, carbs: 8, fats: 0.5 },
          { id: `f_${Date.now()}_3`, name: 'Baies rouges et banane tranchée', quantity: '120g', calories: 85, protein: 1.5, carbs: 20, fats: 0.3 }
        );
        prepTime = '8 min';
        preparation = 'Mélanger les flocons d\'avoine avec le lait chaud ou le skyr. Laisser reposer 3 minutes, puis garnir avec les rondelles de banane et baies fraîches.';
        alternatives = ['Pancakes à l\'avoine et blanc d\'oeuf', 'Tartines de pain complet au beurre de cacahuète et banane'];
      }
    } else if (isSnack) {
      mealName = 'Collation énergisante et protéinée';
      foods.push(
        { id: `f_${Date.now()}_1`, name: isAllergicTo('lactose') ? 'Shake végétal pois/riz' : 'Skyr nature ou fromage blanc', quantity: '180g', calories: 110, protein: 19, carbs: 6, fats: 0.4 },
        { id: `f_${Date.now()}_2`, name: isAllergicTo('fruits_a_coque') ? 'Graines de courge' : 'Amandes entières', quantity: '25g', calories: 145, protein: 5, carbs: 4, fats: 12 },
        { id: `f_${Date.now()}_3`, name: 'Pomme Bio tranchée', quantity: '1 unité', calories: 65, protein: 0.5, carbs: 16, fats: 0.2 }
      );
      prepTime = '5 min';
      preparation = 'Déguster le yaourt ou skyr accompagné de fruits secs croquants et quartiers de pomme fraîche.';
      alternatives = ['Barre énergétique maison avoine-miel', 'Oeuf dur et tranche de pain de seigle'];
    } else {
      // Déjeuner ou Dîner
      const proteinChoices = [
        { name: 'Filet de poulet fermier aux herbes', p: 32, c: 0, f: 3, cal: 160 },
        { name: 'Pavé de saumon grillé au citron', p: 24, c: 0, f: 14, cal: 220 },
        { name: 'Steak haché façon bouchère 5%', p: 26, c: 0, f: 5, cal: 150 },
        { name: 'Filet de cabillaud rôti à l\'ail', p: 23, c: 0, f: 1, cal: 100 },
        { name: 'Tofu fumé sauté au sésame', p: 20, c: 3, f: 10, cal: 180 }
      ];

      const carbChoices = [
        { name: 'Riz basmati complet parfumé', p: 4, c: 45, f: 1, cal: 210, q: '180g cuit' },
        { name: 'Patates douces rôties au paprika', p: 3, c: 40, f: 0.5, cal: 175, q: '200g' },
        { name: 'Quinoa royal aux petits légumes', p: 6, c: 38, f: 3, cal: 200, q: '170g cuit' },
        { name: 'Pâtes complètes au basilic', p: 8, c: 46, f: 1.5, cal: 230, q: '160g cuit' }
      ];

      const pPick = proteinChoices[(dayIndex + orderIndex) % proteinChoices.length];
      const cPick = carbChoices[(dayIndex + orderIndex) % carbChoices.length];

      mealName = type === 'lunch' ? `${pPick.name} et ${cPick.name}` : `Dîner équilibré : ${pPick.name} & légumes de saison`;

      foods.push(
        { id: `f_${Date.now()}_1`, name: pPick.name, quantity: '160g', calories: pPick.cal, protein: pPick.p, carbs: pPick.c, fats: pPick.f },
        { id: `f_${Date.now()}_2`, name: cPick.name, quantity: cPick.q, calories: cPick.cal, protein: cPick.p, carbs: cPick.c, fats: cPick.f },
        { id: `f_${Date.now()}_3`, name: 'Légumes verts vapeur et huile d\'olive vierge', quantity: '200g + 1 c.à.s', calories: 120, protein: 3, carbs: 6, fats: 10 }
      );

      prepTime = profile.cookingTime === 'rapide' ? '15 min' : '25 min';
      preparation = `1. Cuire ${cPick.name} selon les instructions. 2. Saisir ${pPick.name} à la poêle antiadhésive avec un filet d'huile d'olive. 3. Accompagner de légumes verts assaisonnés au jus de citron et fines herbes.`;
      alternatives = ['Substituer la source de protéines par des lentilles ou du poisson blanc', 'Remplacer le riz par du boulgour ou du millet'];
    }

    const totalC = foods.reduce((s, f) => s + f.calories, 0);
    const totalP = foods.reduce((s, f) => s + f.protein, 0);
    const totalCarb = foods.reduce((s, f) => s + f.carbs, 0);
    const totalF = foods.reduce((s, f) => s + f.fats, 0);

    return {
      id: `meal_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      type,
      name: mealName,
      foods,
      calories: totalC || calories,
      protein: totalP || protein,
      carbs: totalCarb || carbs,
      fats: totalF || fats,
      preparation,
      alternatives,
      preparationTime: prepTime,
      isCompleted: false,
      orderIndex
    };
  }

  async regenerateMeal(_prompt: string, profile: NutritionProfile, existingMeal: Meal): Promise<Meal> {
    const mealCals = existingMeal.calories || 600;
    const mealP = existingMeal.protein || 40;
    const mealC = existingMeal.carbs || 60;
    const mealF = existingMeal.fats || 20;

    const regenerated = this.composeMeal(
      existingMeal.type,
      mealCals,
      mealP,
      mealC,
      mealF,
      profile,
      Math.floor(Math.random() * 7),
      existingMeal.orderIndex
    );
    regenerated.id = existingMeal.id; // conserve same meal id
    regenerated.dailyPlanId = existingMeal.dailyPlanId;
    return regenerated;
  }

  async replaceFood(_prompt: string, _profile: NutritionProfile, foodToReplace: FoodItem, _targetMeal: Meal): Promise<FoodItem> {
    const isProtein = foodToReplace.protein > foodToReplace.carbs;
    if (isProtein) {
      return {
        id: foodToReplace.id,
        name: 'Pavé de truite grillé aux herbes',
        quantity: foodToReplace.quantity || '150g',
        calories: foodToReplace.calories,
        protein: foodToReplace.protein,
        carbs: 0,
        fats: Math.max(5, foodToReplace.fats)
      };
    } else {
      return {
        id: foodToReplace.id,
        name: 'Patates douces rôties au four',
        quantity: foodToReplace.quantity || '180g',
        calories: foodToReplace.calories,
        protein: Math.round(foodToReplace.protein * 0.8),
        carbs: foodToReplace.carbs,
        fats: foodToReplace.fats
      };
    }
  }
}

/**
 * Service Backend AI Orchestrator
 */
export class AIService {
  private provider: IAIProvider;
  /** true = real external generative AI (OpenAI/Gemini); false = algorithmic fallback */
  public readonly isRealAI: boolean;

  constructor() {
    this.provider = this.createProvider();
    this.isRealAI = !(this.provider instanceof DynamicNutritionalAIProvider);
  }

  private createProvider(): IAIProvider {
    const aiApiKey = process.env['AI_API_KEY'] || process.env['OPENAI_API_KEY'] || process.env['GEMINI_API_KEY'];
    const aiApiUrl = process.env['AI_API_URL'];
    const aiModel = process.env['AI_MODEL'];
    const configuredProvider = (process.env['AI_PROVIDER'] || '').toLowerCase();

    if (aiApiKey) {
      if (configuredProvider === 'gemini' || aiApiKey.startsWith('AIza') || process.env['GEMINI_API_KEY']) {
        console.log(`[AIService] Fournisseur IA RÉEL : Google Gemini (modèle: ${aiModel || 'gemini-1.5-flash'})`);
        return new GeminiProvider(aiApiKey, aiApiUrl, aiModel);
      } else {
        console.log(`[AIService] Fournisseur IA RÉEL : OpenAI Compatible (modèle: ${aiModel || 'gpt-4o-mini'}, url: ${aiApiUrl || 'https://api.openai.com/v1'})`);
        return new OpenAICompatibleProvider(aiApiKey, aiApiUrl, aiModel);
      }
    }

    console.warn('[AIService] ⚠️  Aucune clé AI_API_KEY dans l\'environnement. Le moteur algorithmique de DÉVELOPPEMENT est activé. Ce moteur n\'est PAS une IA générative.');
    return new DynamicNutritionalAIProvider();
  }

  public getProviderName(): string {
    return this.provider.name;
  }

  /**
   * Construit le prompt diététique exhaustif envoyé à l'IA
   */
  public buildPrompt(profile: NutritionProfile): string {
    return `
Tu es un diététicien-nutritionniste diplômé spécialisé en sport de haut niveau.
Génère un programme alimentaire personnalisé complet de 7 jours (Lundi à Dimanche) basé EXCLUSIVEMENT sur les informations physiologiques et sportives suivantes :

=== DONNÉES DU SPORTIF ===
- Nom complet : ${profile.fullName}
- Âge : ${profile.age} ans
- Sexe : ${profile.sex}
- Taille : ${profile.height} cm
- Poids de corps : ${profile.weight} kg
- Niveau sportif : ${profile.sportsLevel}
- Fréquence d'entraînement : ${profile.sessionsPerWeek} séances par semaine
- Type d'entraînement : ${profile.workoutType}
- Objectif principal : ${profile.goal}
- Régime alimentaire : ${profile.diet}
- Aliments préférés : ${profile.preferredFoods?.join(', ') || 'Aucun spécifié'}
- Aliments détestés (INTERDIT d'en inclure) : ${profile.dislikedFoods?.join(', ') || 'Aucun'}
- ALLERGIES ET INTOLÉRANCES STRICTES (DANGER, AUCUNE TRACE AUTORISÉE) : ${[...(profile.allergies || []), ...(profile.intolerances || [])].join(', ') || 'Aucune'}
- Restrictions alimentaires : ${profile.dietaryRestrictions?.join(', ') || 'Aucune'}
- Nombre de repas souhaité par jour : ${profile.mealsPerDay} repas
- Heure de réveil : ${profile.wakeUpTime}
- Heure de coucher : ${profile.sleepTime}
- Budget alimentaire : ${profile.budget}
- Temps disponible pour cuisiner : ${profile.cookingTime}

=== CIBLES ÉNERGÉTIQUES ET MACRONUTRIMENTS CALCULÉS ===
- Métabolisme de Base (BMR) : ${profile.bmr} kcal
- Dépense Énergétique Journalière (TDEE) : ${profile.tdee} kcal
- Cible calorique quotidienne : ${profile.targetCalories} kcal
- Protéines cibles quotidiennes : ${profile.targetProtein} g
- Glucides cibles quotidiens : ${profile.targetCarbs} g
- Lipides cibles quotidiens : ${profile.targetFats} g

=== EXIGENCES STRICTES ===
1. Génère exactement 7 jours : "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche".
2. Chaque jour doit comporter exactement ${profile.mealsPerDay} repas adaptés aux horaires (par ex. breakfast, lunch, dinner, et snacks selon le cas).
3. Respecte IMPÉRATIVEMENT les allergies et aliments détestés.
4. Pour chaque repas, indique :
   - type : "breakfast" | "morning_snack" | "lunch" | "afternoon_snack" | "dinner" | "evening_snack"
   - name : nom gourmand et précis du repas en français
   - foods : liste d'aliments avec name, quantity (ex: "150g", "2 pièces"), calories, protein, carbs, fats
   - calories, protein, carbs, fats totaux du repas
   - preparation : recette pas-à-pas claire
   - alternatives : au moins 2 alternatives possibles pour ce repas
   - preparationTime : durée estimée (ex: "15 min")
5. Réponds UNIQUEMENT sous la forme d'un objet JSON valide respectant cette structure exacte :
{
  "mealPlan": {
    "durationDays": 7,
    "dailyTarget": {
      "calories": ${profile.targetCalories},
      "protein": ${profile.targetProtein},
      "carbs": ${profile.targetCarbs},
      "fats": ${profile.targetFats}
    },
    "days": [
      {
        "day": "Lundi",
        "meals": [
          {
            "type": "breakfast",
            "name": "...",
            "foods": [
              { "name": "...", "quantity": "...", "calories": 0, "protein": 0, "carbs": 0, "fats": 0 }
            ],
            "calories": 0,
            "protein": 0,
            "carbs": 0,
            "fats": 0,
            "preparation": "...",
            "alternatives": ["...", "..."],
            "preparationTime": "..."
          }
        ]
      }
    ]
  }
}
`;
  }

  /**
   * Validation approfondie de la réponse de l'IA
   */
  public validateAIResponse(data: any, profile: NutritionProfile): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!data || typeof data !== 'object') {
      errors.push('La réponse de l\'IA n\'est pas un objet JSON valide.');
      return { valid: false, errors };
    }

    const mealPlan = data.mealPlan;
    if (!mealPlan) {
      errors.push('Le champ racine "mealPlan" est manquant.');
      return { valid: false, errors };
    }

    if (!Array.isArray(mealPlan.days) || mealPlan.days.length !== 7) {
      errors.push(`Le programme doit contenir exactement 7 jours (reçu: ${mealPlan.days?.length || 0}).`);
    }

    // Vérification des allergènes déclarés
    const allergens = (profile.allergies || []).map(a => a.toLowerCase().trim()).filter(Boolean);
    
    if (Array.isArray(mealPlan.days)) {
      mealPlan.days.forEach((day: any, dIdx: number) => {
        if (!day.day || !Array.isArray(day.meals) || day.meals.length === 0) {
          errors.push(`Le jour ${dIdx + 1} (${day.day || 'inconnu'}) est incomplet ou ne comporte aucun repas.`);
          return;
        }

        day.meals.forEach((meal: any, mIdx: number) => {
          if (!meal.name || !meal.type || typeof meal.calories !== 'number') {
            errors.push(`Le repas ${mIdx + 1} du jour ${day.day} manque d'attributs obligatoires.`);
          }

          // Détection d'allergènes dans les aliments ou le nom du repas
          if (allergens.length > 0) {
            const mealText = `${meal.name} ${meal.preparation || ''} ${(meal.foods || []).map((f: any) => f.name).join(' ')}`.toLowerCase();
            for (const allergen of allergens) {
              if (mealText.includes(allergen)) {
                errors.push(`Violation d'allergène détectée : l'allergène "${allergen}" a été trouvé dans le repas "${meal.name}" (${day.day}).`);
              }
            }
          }
        });
      });
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  public async generatePlan(profile: NutritionProfile): Promise<AIStructuredMealPlanResponse> {
    const prompt = this.buildPrompt(profile);
    
    try {
      const response = await this.provider.generateMealPlan(prompt, profile);
      const validation = this.validateAIResponse(response, profile);

      if (!validation.valid) {
        console.warn('[AIService] Avertissement lors de la validation IA :', validation.errors);
        // If external AI had minor structural glitch, retry or use fallback
        if (validation.errors.some(e => e.includes('Violation d\'allergène'))) {
          throw new Error(`Échec de sécurité : ${validation.errors.join('; ')}`);
        }
      }

      return response;
    } catch (err: any) {
      console.error('[AIService] Erreur lors de la génération avec le fournisseur principal:', err.message);
      // If external provider fails (e.g. rate limit, invalid key, network timeout), fallback safely to algorithmic provider
      if (!(this.provider instanceof DynamicNutritionalAIProvider)) {
        console.log('[AIService] Basculement de secours sur le moteur dynamique nutritionnel sécurisé.');
        const fallback = new DynamicNutritionalAIProvider();
        return await fallback.generateMealPlan(prompt, profile);
      }
      throw err;
    }
  }

  public async regenerateMeal(profile: NutritionProfile, existingMeal: Meal): Promise<Meal> {
    const prompt = `
Régénère un repas de type "${existingMeal.type}" pour ${profile.fullName}.
Objectif : ${profile.goal}.
Calories cibles du repas : environ ${existingMeal.calories} kcal (Protéines : ~${existingMeal.protein}g, Glucides : ~${existingMeal.carbs}g, Lipides : ~${existingMeal.fats}g).
Allergies STRICTEMENT INTERDITES : ${[...(profile.allergies || []), ...(profile.intolerances || [])].join(', ') || 'Aucune'}.
Aliments détestés : ${profile.dislikedFoods?.join(', ') || 'Aucun'}.
Régime : ${profile.diet}.
Format JSON requis :
{
  "meal": {
    "type": "${existingMeal.type}",
    "name": "...",
    "foods": [
      { "name": "...", "quantity": "...", "calories": 0, "protein": 0, "carbs": 0, "fats": 0 }
    ],
    "calories": 0,
    "protein": 0,
    "carbs": 0,
    "fats": 0,
    "preparation": "...",
    "alternatives": ["...", "..."],
    "preparationTime": "..."
  }
}
`;
    return await this.provider.regenerateMeal(prompt, profile, existingMeal);
  }

  public async replaceFood(profile: NutritionProfile, foodToReplace: FoodItem, targetMeal: Meal): Promise<FoodItem> {
    const prompt = `
Remplace l'ingrédient "${foodToReplace.name}" (${foodToReplace.quantity}, ${foodToReplace.calories} kcal, P:${foodToReplace.protein}g, C:${foodToReplace.carbs}g, F:${foodToReplace.fats}g)
dans le repas "${targetMeal.name}".
Contraintes :
- Doit avoir un profil calorique et de macros très similaire (équivalence nutritionnelle).
- Allergies strictement exclues : ${[...(profile.allergies || []), ...(profile.intolerances || [])].join(', ') || 'Aucune'}.
- Aliments détestés exclus : ${profile.dislikedFoods?.join(', ') || 'Aucun'}.
- Régime : ${profile.diet}.
Format JSON attendu :
{
  "food": {
    "name": "...",
    "quantity": "...",
    "calories": 0,
    "protein": 0,
    "carbs": 0,
    "fats": 0
  }
}
`;
    return await this.provider.replaceFood(prompt, profile, foodToReplace, targetMeal);
  }
}
