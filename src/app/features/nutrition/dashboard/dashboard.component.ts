import { Component, OnInit, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NutritionService } from '../../../core/services/nutrition.service';
import { MealPlanService } from '../../../core/services/meal-plan.service';
import { AIService } from '../../../core/services/ai.service';
import { Meal } from '../../../core/models/nutrition.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="max-w-6xl mx-auto px-4 py-8">
      
      <!-- En-tête du Dashboard -->
      <div class="flex flex-col md:flex-row md:items-center justify-between pb-6 mb-8 border-b border-slate-200">
        <div>
          <span class="text-xs uppercase tracking-widest font-extrabold text-emerald-600">
            Site Web d'Entraînement • Module Nutrition IA
          </span>
          <h1 class="text-2xl md:text-3xl font-black text-slate-900 tracking-tight mt-1">
            Tableau de Bord Nutritionnel
          </h1>
          <p class="text-sm text-slate-500 mt-1">
            Bienvenue, {{ profile()?.fullName || 'Sportif' }} ! Voici le suivi de vos objectifs et votre programme actif.
          </p>
        </div>

        <div class="mt-4 md:mt-0 flex items-center gap-3">
          <a 
            routerLink="/profile"
            class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition">
            👤 Modifier mon profil
          </a>
          <a 
            routerLink="/meal-plan"
            [queryParams]="{ generate: 'true' }"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-md transition transform active:scale-95">
            ⚡ Générer avec l'IA
          </a>
        </div>
      </div>

      <!-- CARTES D'OBJECTIF & MACRONUTRIMENTS -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        
        <!-- Carte Calories -->
        <div class="bg-gradient-to-br from-slate-900 to-slate-800 text-white rounded-3xl p-6 shadow-md border border-slate-800 flex flex-col justify-between">
          <div class="flex items-center justify-between">
            <span class="text-xs uppercase tracking-wider font-bold text-emerald-400">Calories Cibles</span>
            <span class="text-xl">🔥</span>
          </div>
          <div class="my-4">
            <div class="text-3xl font-black text-white">
              {{ profile()?.targetCalories || 2400 }}
              <span class="text-sm font-bold text-slate-400">kcal/j</span>
            </div>
            <div class="text-xs text-slate-400 mt-1">
              BMR: {{ profile()?.bmr || 1700 }} • TDEE: {{ profile()?.tdee || 2400 }}
            </div>
          </div>
          <div class="w-full bg-slate-700/60 rounded-full h-2">
            <div class="bg-emerald-400 h-2 rounded-full w-full"></div>
          </div>
        </div>

        <!-- Carte Protéines -->
        <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-200/80 flex flex-col justify-between">
          <div class="flex items-center justify-between">
            <span class="text-xs uppercase tracking-wider font-bold text-sky-600">Protéines</span>
            <span class="text-xl">🥩</span>
          </div>
          <div class="my-4">
            <div class="text-3xl font-black text-slate-900">
              {{ profile()?.targetProtein || 150 }}
              <span class="text-sm font-bold text-slate-400">g</span>
            </div>
            <div class="text-xs text-slate-500 mt-1">
              {{ (profile()?.targetProtein || 150) * 4 }} kcal ({{ getMacroPercent('p') }}% de l'apport)
            </div>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2">
            <div class="bg-sky-500 h-2 rounded-full" [style.width]="getMacroPercent('p') + '%'"></div>
          </div>
        </div>

        <!-- Carte Glucides -->
        <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-200/80 flex flex-col justify-between">
          <div class="flex items-center justify-between">
            <span class="text-xs uppercase tracking-wider font-bold text-amber-600">Glucides</span>
            <span class="text-xl">🍚</span>
          </div>
          <div class="my-4">
            <div class="text-3xl font-black text-slate-900">
              {{ profile()?.targetCarbs || 250 }}
              <span class="text-sm font-bold text-slate-400">g</span>
            </div>
            <div class="text-xs text-slate-500 mt-1">
              {{ (profile()?.targetCarbs || 250) * 4 }} kcal ({{ getMacroPercent('c') }}% de l'apport)
            </div>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2">
            <div class="bg-amber-500 h-2 rounded-full" [style.width]="getMacroPercent('c') + '%'"></div>
          </div>
        </div>

        <!-- Carte Lipides -->
        <div class="bg-white rounded-3xl p-6 shadow-sm border border-slate-200/80 flex flex-col justify-between">
          <div class="flex items-center justify-between">
            <span class="text-xs uppercase tracking-wider font-bold text-rose-600">Lipides</span>
            <span class="text-xl">🥑</span>
          </div>
          <div class="my-4">
            <div class="text-3xl font-black text-slate-900">
              {{ profile()?.targetFats || 70 }}
              <span class="text-sm font-bold text-slate-400">g</span>
            </div>
            <div class="text-xs text-slate-500 mt-1">
              {{ (profile()?.targetFats || 70) * 9 }} kcal ({{ getMacroPercent('f') }}% de l'apport)
            </div>
          </div>
          <div class="w-full bg-slate-100 rounded-full h-2">
            <div class="bg-rose-500 h-2 rounded-full" [style.width]="getMacroPercent('f') + '%'"></div>
          </div>
        </div>

      </div>

      <!-- SECTION CENTRALE : STATUT DU PROFIL & REPAS DU JOUR -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        <!-- Colonne Gauche : Aperçu du Profil Actif (1 col) -->
        <div class="space-y-6">
          <div class="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-sm">
            <div class="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
              <h3 class="font-bold text-base text-slate-900 flex items-center gap-2">
                <span>👤</span> Profil Sportif Actuel
              </h3>
              <a routerLink="/profile" class="text-xs font-bold text-emerald-600 hover:text-emerald-800">
                Modifier →
              </a>
            </div>

            <div class="space-y-3.5 text-sm">
              <div class="flex justify-between items-center py-1 border-b border-slate-50">
                <span class="text-slate-500 text-xs uppercase font-medium">Objectif</span>
                <span class="font-bold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-lg text-xs">
                  {{ getGoalLabel(profile()?.goal) }}
                </span>
              </div>

              <div class="flex justify-between items-center py-1 border-b border-slate-50">
                <span class="text-slate-500 text-xs uppercase font-medium">Morphologie</span>
                <span class="font-semibold text-slate-800">
                  {{ profile()?.weight }} kg • {{ profile()?.height }} cm ({{ profile()?.age }} ans)
                </span>
              </div>

              <div class="flex justify-between items-center py-1 border-b border-slate-50">
                <span class="text-slate-500 text-xs uppercase font-medium">Sport & Fréquence</span>
                <span class="font-semibold text-slate-800 capitalize">
                  {{ profile()?.workoutType }} ({{ profile()?.sessionsPerWeek }}x / sem)
                </span>
              </div>

              <div class="flex justify-between items-center py-1 border-b border-slate-50">
                <span class="text-slate-500 text-xs uppercase font-medium">Régime</span>
                <span class="font-semibold text-slate-800 capitalize">
                  {{ profile()?.diet }}
                </span>
              </div>

              <div class="flex justify-between items-center py-1">
                <span class="text-slate-500 text-xs uppercase font-medium">Repas par jour</span>
                <span class="font-semibold text-slate-800">
                  {{ profile()?.mealsPerDay }} repas
                </span>
              </div>
            </div>

            <!-- Allergies déclarées -->
            @if (profile()?.allergies && profile()!.allergies.length > 0) {
              <div class="mt-5 pt-4 border-t border-slate-100">
                <span class="text-xs uppercase font-bold text-rose-600 block mb-2">
                  🚫 Allergies strictement bannies :
                </span>
                <div class="flex flex-wrap gap-1.5">
                  @for (a of profile()!.allergies; track a) {
                    <span class="px-2.5 py-1 rounded-lg bg-rose-50 text-rose-700 text-xs font-semibold">
                      {{ a }}
                    </span>
                  }
                </div>
              </div>
            }
          </div>

          <!-- Carte Recommandation Hydratation & Conseils -->
          <div class="bg-gradient-to-br from-emerald-500/10 via-teal-500/5 to-cyan-500/10 rounded-3xl p-6 border border-emerald-500/20">
            <div class="flex items-center gap-3 mb-3">
              <span class="text-2xl">💧</span>
              <h4 class="font-bold text-slate-900 text-sm">Hydratation Recommandée</h4>
            </div>
            <p class="text-xs text-slate-600 leading-relaxed mb-3">
              Pour un sportif de <strong>{{ profile()?.weight || 70 }} kg</strong> effectuant <strong>{{ profile()?.sessionsPerWeek || 3 }} séances</strong>, buvez environ <strong>{{ ((profile()?.weight || 70) * 0.04).toFixed(1) }} litres</strong> d'eau par jour pour optimiser la récupération et l'assimilation des nutriments.
            </p>
          </div>
        </div>

        <!-- Colonne Droite : Repas du Jour (Aujourd'hui) (2 cols) -->
        <div class="lg:col-span-2 space-y-6">
          <div class="bg-white rounded-3xl border border-slate-200/80 p-6 shadow-sm">
            <div class="flex items-center justify-between pb-4 border-b border-slate-100 mb-6">
              <div>
                <h3 class="font-black text-lg text-slate-900">
                  🍽️ Repas du Jour (Lundi)
                </h3>
                <p class="text-xs text-slate-500 mt-0.5">
                  Extrait de votre programme 7 jours actif
                </p>
              </div>

              <a routerLink="/meal-plan" class="text-xs font-bold text-emerald-600 hover:text-emerald-800">
                Voir les 7 jours complets →
              </a>
            </div>

            @if (todayMeals().length === 0) {
              <div class="text-center py-10">
                <span class="text-3xl block mb-2">✨</span>
                <p class="text-sm font-semibold text-slate-700">Aucun repas généré pour le moment.</p>
                <p class="text-xs text-slate-400 mt-1 mb-4">Créez votre programme de la semaine avec l'IA en un clic.</p>
                <a 
                  routerLink="/meal-plan" 
                  [queryParams]="{ generate: 'true' }"
                  class="inline-block px-5 py-2.5 rounded-xl bg-slate-900 text-white font-bold text-xs">
                  Générer maintenant
                </a>
              </div>
            } @else {
              <div class="space-y-4">
                @for (meal of todayMeals(); track meal.id) {
                  <div 
                    class="p-4 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                    [class.border-emerald-300]="meal.isCompleted"
                    [class.bg-emerald-50/20]="meal.isCompleted"
                    [class.border-slate-200/70]="!meal.isCompleted">
                    
                    <div class="flex items-center gap-3">
                      <span class="text-2xl">{{ getMealEmoji(meal.type) }}</span>
                      <div>
                        <div class="flex items-center gap-2">
                          <span class="text-[11px] font-bold uppercase tracking-wider text-emerald-600">
                            {{ getMealTypeLabel(meal.type) }}
                          </span>
                          @if (meal.isCompleted) {
                            <span class="px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                              ✓ Terminé
                            </span>
                          }
                        </div>
                        <div class="font-bold text-sm text-slate-900 mt-0.5">
                          {{ meal.name }}
                        </div>
                        <div class="text-xs text-slate-500 mt-1 flex flex-wrap gap-2">
                          <span>🔥 {{ meal.calories }} kcal</span>
                          <span>• P: {{ meal.protein }}g</span>
                          <span>• G: {{ meal.carbs }}g</span>
                          <span>• L: {{ meal.fats }}g</span>
                        </div>
                      </div>
                    </div>

                    <div class="flex items-center gap-2 self-end sm:self-center">
                      <button 
                        type="button"
                        (click)="toggleMeal(meal)"
                        class="px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer"
                        [class.bg-emerald-600]="!meal.isCompleted"
                        [class.text-white]="!meal.isCompleted"
                        [class.bg-slate-200]="meal.isCompleted"
                        [class.text-slate-700]="meal.isCompleted">
                        {{ meal.isCompleted ? 'Annuler' : '✓ Fait' }}
                      </button>
                      <a 
                        routerLink="/meal-plan"
                        class="px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold">
                        Détails
                      </a>
                    </div>
                  </div>
                }
              </div>
            }

          </div>
        </div>

      </div>

    </div>
  `
})
export class DashboardComponent implements OnInit {
  public nutritionService = inject(NutritionService);
  public mealPlanService = inject(MealPlanService);
  public aiService = inject(AIService);

  public profile = this.nutritionService.currentProfile;
  public mealPlan = this.mealPlanService.currentPlan;

  public todayMeals = computed<Meal[]>(() => {
    const plan = this.mealPlan();
    if (!plan || !plan.days || plan.days.length === 0) return [];
    return plan.days[0].meals || [];
  });

  ngOnInit(): void {
    this.nutritionService.loadProfile().subscribe({ error: () => {} });
    this.mealPlanService.loadCurrentPlan().subscribe({
      error: () => {}
    });
  }

  toggleMeal(meal: Meal): void {
    this.mealPlanService.toggleMealCompleted(meal.id, !meal.isCompleted).subscribe();
  }

  getMacroPercent(type: 'p' | 'c' | 'f'): number {
    const prof = this.profile();
    if (!prof) return 33;
    const cal = prof.targetCalories || 2000;
    if (type === 'p') return Math.round(((prof.targetProtein * 4) / cal) * 100);
    if (type === 'c') return Math.round(((prof.targetCarbs * 4) / cal) * 100);
    if (type === 'f') return Math.round(((prof.targetFats * 9) / cal) * 100);
    return 33;
  }

  getGoalLabel(goal?: string): string {
    const map: Record<string, string> = {
      perte_de_poids: 'Perte de poids',
      prise_de_masse: 'Prise de masse',
      maintien: 'Maintien',
      performance: 'Performance',
      reequilibrage: 'Rééquilibrage'
    };
    return (goal && map[goal]) ? map[goal] : (goal || 'Maintien');
  }

  getMealEmoji(type: string): string {
    switch (type) {
      case 'breakfast': return '🍳';
      case 'lunch': return '🥗';
      case 'dinner': return '🍲';
      default: return '🍎';
    }
  }

  getMealTypeLabel(type: string): string {
    switch (type) {
      case 'breakfast': return 'Petit-déjeuner';
      case 'lunch': return 'Déjeuner';
      case 'dinner': return 'Dîner';
      case 'morning_snack': return 'Collation matin';
      case 'afternoon_snack': return 'Collation après-midi';
      case 'evening_snack': return 'Collation soir';
      default: return 'Repas';
    }
  }
}
