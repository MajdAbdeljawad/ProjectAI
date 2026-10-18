import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MealPlanService } from '../../../core/services/meal-plan.service';
import { NutritionService } from '../../../core/services/nutrition.service';
import { AIService } from '../../../core/services/ai.service';
import { DailyPlan, FoodItem, Meal, MealPlan } from '../../../core/models/nutrition.models';

@Component({
  selector: 'app-meal-plan',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="max-w-6xl mx-auto px-4 py-8">
      
      <!-- En-tête de la page -->
      <div class="flex flex-col md:flex-row md:items-center justify-between pb-6 mb-8 border-b border-slate-200">
        <div>
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-600 font-bold text-xl">
              📅
            </div>
            <div>
              <h1 class="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight">
                Programme Alimentaire Personnalisé
              </h1>
              <!-- Sous-titre honnête selon le statut IA -->
              @if (aiService.aiStatus()?.isRealAI) {
                <p class="text-sm text-emerald-600 font-semibold mt-0.5 flex items-center gap-1.5">
                  <span class="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  Généré par IA générative ({{ aiService.aiStatus()?.providerName }})
                </p>
              } @else if (aiService.aiStatus()) {
                <p class="text-sm text-amber-600 font-semibold mt-0.5 flex items-center gap-1.5">
                  <span class="inline-block w-2 h-2 rounded-full bg-amber-500"></span>
                  Mode développement — Configuration API IA requise
                </p>
              } @else {
                <p class="text-sm text-slate-500 mt-0.5">
                  Programme nutritionnel sur-mesure adapté à votre profil.
                </p>
              }
            </div>
          </div>
        </div>

        <div class="mt-4 md:mt-0 flex items-center gap-3">
          <button 
            type="button"
            (click)="regenerateAll()"
            [disabled]="aiService.isGenerating()"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm shadow-sm transition-all transform active:scale-95 cursor-pointer disabled:opacity-50">
            <span>✨</span>
            {{ mealPlan() ? 'Régénérer tout le programme' : 'Générer mon programme alimentaire' }}
          </button>
        </div>
      </div>

      <!-- ÉTAT DE CHARGEMENT MULTI-ÉTAPES IA (Requirement 17) -->
      @if (aiService.isGenerating()) {
        <div class="mb-8 p-8 rounded-3xl bg-slate-950 text-white shadow-2xl border border-slate-800 animate-fadeIn">
          <div class="max-w-xl mx-auto text-center space-y-6">
            <div class="inline-flex p-4 rounded-full bg-emerald-500/10 text-emerald-400 text-3xl animate-bounce">
              🤖
            </div>

            <div>
              <h3 class="text-xl font-bold tracking-tight text-white mb-2">
                Génération de votre programme par l'IA
              </h3>
              <p class="text-sm text-emerald-400 font-semibold h-6 transition-all duration-300">
                {{ aiService.currentStepMessage() }}
              </p>
            </div>

            <!-- Barre de progression -->
            <div class="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
              <div 
                class="bg-gradient-to-r from-emerald-500 via-teal-400 to-sky-400 h-2.5 rounded-full transition-all duration-700 ease-out"
                [style.width]="(aiService.currentStep() / 6) * 100 + '%'">
              </div>
            </div>

            <!-- Liste des étapes séquentielles -->
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2 text-left pt-2">
              @for (step of steps; track step.index) {
                <div 
                  class="p-2.5 rounded-xl text-xs flex items-center gap-2 border transition-all"
                  [class.bg-emerald-950/40]="aiService.currentStep() >= step.index"
                  [class.border-emerald-500/40]="aiService.currentStep() >= step.index"
                  [class.text-emerald-300]="aiService.currentStep() >= step.index"
                  [class.bg-slate-900]="aiService.currentStep() < step.index"
                  [class.border-slate-800]="aiService.currentStep() < step.index"
                  [class.text-slate-500]="aiService.currentStep() < step.index">
                  @if (aiService.currentStep() > step.index) {
                    <span class="text-emerald-400 font-bold">✓</span>
                  } @else if (aiService.currentStep() === step.index) {
                    <div class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping"></div>
                  } @else {
                    <span class="text-slate-600 font-bold">{{ step.index }}</span>
                  }
                  <span class="truncate font-medium">{{ step.title }}</span>
                </div>
              }
            </div>
          </div>
        </div>
      }

      <!-- Message d'erreur -->
      @if (aiService.error()) {
        <div class="mb-8 p-5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="text-2xl">⚠️</span>
            <div>
              <div class="font-bold text-sm">Échec de la génération IA</div>
              <div class="text-xs text-rose-600 mt-0.5">{{ aiService.error() }}</div>
            </div>
          </div>
          <button 
            type="button"
            (click)="regenerateAll()" 
            class="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold cursor-pointer">
            Réessayer
          </button>
        </div>
      }

      <!-- ÉTAT VIDE (EMPTY STATE) -->
      @if (!mealPlan() && !aiService.isGenerating()) {
        <div class="text-center py-16 px-4 bg-white rounded-3xl border border-slate-200/80 shadow-sm max-w-2xl mx-auto">
          <div class="w-20 h-20 mx-auto rounded-3xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-4xl mb-5">
            🥗
          </div>
          <h2 class="text-2xl font-black text-slate-900 tracking-tight mb-2">
            Aucun programme alimentaire actif
          </h2>

          <!-- Bandeau état de l'IA -->
          @if (aiService.aiStatus()?.isRealAI) {
            <div class="inline-flex items-center gap-2 px-4 py-2 mb-4 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-bold">
              <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse inline-block"></span>
              IA générative active — {{ aiService.aiStatus()?.providerName }}
            </div>
            <p class="text-sm text-slate-500 max-w-md mx-auto mb-6">
              Votre profil est configuré. Cliquez ci-dessous pour laisser l'IA générer vos 7 jours de repas personnalisés.
            </p>
          } @else if (aiService.aiStatus()) {
            <div class="max-w-lg mx-auto mb-5 p-4 rounded-2xl bg-amber-50 border border-amber-200 text-left">
              <div class="flex gap-3">
                <span class="text-xl shrink-0">⚠️</span>
                <div>
                  <div class="font-bold text-amber-800 text-sm">Configuration API IA requise</div>
                  <p class="text-xs text-amber-700 mt-1">
                    Aucune clé API IA n'est configurée côté serveur. Le programme sera généré par un moteur algorithmique de développement, <strong>pas par une IA générative</strong>.<br>
                    Pour activer la génération IA réelle, ajoutez <code class="bg-amber-100 px-1 rounded">AI_API_KEY</code> dans le fichier <code class="bg-amber-100 px-1 rounded">.env</code> du serveur (OpenAI, Gemini, Groq…).
                  </p>
                </div>
              </div>
            </div>
            <p class="text-sm text-slate-500 max-w-md mx-auto mb-6">
              Vous pouvez tout de même générer un programme adapté à votre profil (calories, macros, allergies) en mode développement.
            </p>
          } @else {
            <p class="text-sm text-slate-500 max-w-md mx-auto mb-6">
              Votre profil nutritionnel est configuré. Cliquez ci-dessous pour générer vos 7 jours de repas.
            </p>
          }
          <div class="flex flex-col sm:flex-row items-center justify-center gap-3">
            <button 
              type="button"
              (click)="regenerateAll()"
              class="w-full sm:w-auto px-6 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-md transition transform active:scale-95 cursor-pointer">
              ⚡ Générer mon programme alimentaire (7 jours)
            </button>
            <a 
              routerLink="/profile"
              class="w-full sm:w-auto px-6 py-3 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition">
              Vérifier mon profil d'abord
            </a>
          </div>
        </div>
      }

      <!-- CONTENU DU PROGRAMME 7 JOURS -->
      @if (mealPlan()) {
        <!-- Barre de résumé des cibles globales -->
        <div class="mb-8 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-sm flex flex-wrap items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-lg">
              🎯
            </div>
            <div>
              <span class="text-xs uppercase tracking-wider font-bold text-slate-400">Objectif Quotidien Ciblé</span>
              <div class="text-lg font-extrabold text-slate-900">
                {{ mealPlan()?.dailyTarget?.calories }} kcal
                <span class="text-xs text-slate-500 font-normal">/ jour</span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-6">
            <div>
              <span class="block text-[11px] font-semibold text-slate-400 uppercase">Protéines</span>
              <span class="text-sm font-bold text-sky-600">{{ mealPlan()?.dailyTarget?.protein }} g</span>
            </div>
            <div class="h-6 w-px bg-slate-200"></div>
            <div>
              <span class="block text-[11px] font-semibold text-slate-400 uppercase">Glucides</span>
              <span class="text-sm font-bold text-amber-600">{{ mealPlan()?.dailyTarget?.carbs }} g</span>
            </div>
            <div class="h-6 w-px bg-slate-200"></div>
            <div>
              <span class="block text-[11px] font-semibold text-slate-400 uppercase">Lipides</span>
              <span class="text-sm font-bold text-rose-600">{{ mealPlan()?.dailyTarget?.fats }} g</span>
            </div>
          </div>
        </div>

        <!-- ONGLETS DES 7 JOURS (Lundi à Dimanche) -->
        <div class="flex items-center gap-2 overflow-x-auto pb-3 mb-6 scrollbar-none">
          @for (day of mealPlan()?.days; track day.dayNumber) {
            <button 
              type="button"
              (click)="selectedDayIndex.set(day.dayNumber - 1)"
              class="px-5 py-3 rounded-2xl font-bold text-sm transition-all whitespace-nowrap cursor-pointer flex flex-col items-center min-w-[110px]"
              [class.bg-emerald-600]="selectedDayIndex() === (day.dayNumber - 1)"
              [class.text-white]="selectedDayIndex() === (day.dayNumber - 1)"
              [class.shadow-md]="selectedDayIndex() === (day.dayNumber - 1)"
              [class.bg-white]="selectedDayIndex() !== (day.dayNumber - 1)"
              [class.text-slate-700]="selectedDayIndex() !== (day.dayNumber - 1)"
              [class.border]="selectedDayIndex() !== (day.dayNumber - 1)"
              [class.border-slate-200]="selectedDayIndex() !== (day.dayNumber - 1)"
              [class.hover:bg-slate-50]="selectedDayIndex() !== (day.dayNumber - 1)">
              <span>{{ day.day }}</span>
              <span 
                class="text-[11px] font-medium mt-0.5"
                [class.text-emerald-100]="selectedDayIndex() === (day.dayNumber - 1)"
                [class.text-slate-400]="selectedDayIndex() !== (day.dayNumber - 1)">
                {{ day.totalCalories }} kcal
              </span>
            </button>
          }
        </div>

        <!-- DÉTAIL DU JOUR SÉLECTIONNÉ -->
        @if (currentDay(); as activeDay) {
          <div class="space-y-6">
            <!-- Bandeau récapitulatif du jour sélectionné -->
            <div class="bg-gradient-to-r from-slate-900 to-slate-800 rounded-2xl p-5 text-white flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 class="text-xl font-black tracking-tight text-white">
                  Menu du {{ activeDay.day }}
                </h2>
                <p class="text-xs text-slate-300 mt-0.5">
                  {{ activeDay.meals.length }} repas prévus • Total de la journée : {{ activeDay.totalCalories }} kcal
                </p>
              </div>

              <!-- Macros du jour -->
              <div class="flex items-center gap-4 text-xs">
                <span class="px-3 py-1.5 rounded-xl bg-sky-500/20 text-sky-300 font-semibold border border-sky-500/30">
                  🥩 P: {{ activeDay.totalProtein }}g
                </span>
                <span class="px-3 py-1.5 rounded-xl bg-amber-500/20 text-amber-300 font-semibold border border-amber-500/30">
                  🍚 G: {{ activeDay.totalCarbs }}g
                </span>
                <span class="px-3 py-1.5 rounded-xl bg-rose-500/20 text-rose-300 font-semibold border border-rose-500/30">
                  🥑 L: {{ activeDay.totalFats }}g
                </span>
              </div>
            </div>

            <!-- LISTE DES REPAS DE LA JOURNÉE -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              @for (meal of activeDay.meals; track meal.id) {
                <div 
                  class="bg-white rounded-2xl border transition-all duration-200 shadow-sm hover:shadow-md flex flex-col justify-between overflow-hidden"
                  [class.border-emerald-300]="meal.isCompleted"
                  [class.bg-emerald-50/20]="meal.isCompleted"
                  [class.border-slate-200/80]="!meal.isCompleted">
                  
                  <div class="p-6">
                    <!-- En-tête du repas -->
                    <div class="flex items-start justify-between gap-3 mb-3">
                      <div class="flex items-center gap-2">
                        <span class="text-2xl">{{ getMealEmoji(meal.type) }}</span>
                        <div>
                          <span class="text-xs font-bold uppercase tracking-wider text-emerald-600">
                            {{ getMealTypeLabel(meal.type) }}
                          </span>
                          <h3 class="text-base font-bold text-slate-900 leading-snug">
                            {{ meal.name }}
                          </h3>
                        </div>
                      </div>

                      <!-- Badge Statut / Complété -->
                      <button 
                        type="button"
                        (click)="toggleMealCompleted(meal)"
                        class="px-2.5 py-1 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5"
                        [class.bg-emerald-100]="meal.isCompleted"
                        [class.text-emerald-800]="meal.isCompleted"
                        [class.bg-slate-100]="!meal.isCompleted"
                        [class.text-slate-600]="!meal.isCompleted"
                        [class.hover:bg-slate-200]="!meal.isCompleted">
                        @if (meal.isCompleted) {
                          <span>✓ Terminé</span>
                        } @else {
                          <span>○ À faire</span>
                        }
                      </button>
                    </div>

                    <!-- Métriques du repas (Calories & Macros) -->
                    <div class="flex items-center gap-2 my-4">
                      <span class="px-2.5 py-1 rounded-lg bg-slate-100 text-slate-800 text-xs font-extrabold">
                        🔥 {{ meal.calories }} kcal
                      </span>
                      <span class="px-2 py-0.5 rounded-lg bg-sky-50 text-sky-700 text-xs font-semibold">
                        P: {{ meal.protein }}g
                      </span>
                      <span class="px-2 py-0.5 rounded-lg bg-amber-50 text-amber-700 text-xs font-semibold">
                        G: {{ meal.carbs }}g
                      </span>
                      <span class="px-2 py-0.5 rounded-lg bg-rose-50 text-rose-700 text-xs font-semibold">
                        L: {{ meal.fats }}g
                      </span>
                      <span class="ml-auto text-xs text-slate-400 font-medium flex items-center gap-1">
                        ⏱️ {{ meal.preparationTime }}
                      </span>
                    </div>

                    <!-- Aperçu des ingrédients -->
                    <div class="space-y-1 pt-2 border-t border-slate-100">
                      @for (food of meal.foods; track food.id) {
                        <div class="flex items-center justify-between text-xs text-slate-600 py-0.5">
                          <span class="font-medium">• {{ food.name }}</span>
                          <span class="text-slate-400">{{ food.quantity }}</span>
                        </div>
                      }
                    </div>
                  </div>

                  <!-- Actions du repas -->
                  <div class="bg-slate-50/80 px-6 py-3.5 border-t border-slate-100 flex items-center justify-between gap-3">
                    <button 
                      type="button"
                      (click)="openMealDetails(meal)"
                      class="text-xs font-bold text-emerald-700 hover:text-emerald-900 cursor-pointer flex items-center gap-1">
                      <span>📖 Recette & Alternatives</span>
                    </button>

                    <button 
                      type="button"
                      (click)="regenerateMeal(meal)"
                      [disabled]="isRegeneratingMeal() === meal.id"
                      class="text-xs font-semibold text-slate-600 hover:text-slate-900 cursor-pointer flex items-center gap-1 disabled:opacity-50">
                      @if (isRegeneratingMeal() === meal.id) {
                        <div class="w-3 h-3 border-2 border-slate-600 border-t-transparent rounded-full animate-spin"></div>
                        <span>Régénération...</span>
                      } @else {
                        <span>🔄 Régénérer (IA)</span>
                      }
                    </button>
                  </div>

                </div>
              }
            </div>
          </div>
        }
      }

      <!-- MODALE DÉTAILS DU REPAS & REMPLACEMENT D'ALIMENT (Requirement 13 & 15) -->
      @if (selectedMeal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-sm animate-fadeIn">
          <div class="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-slate-200">
            
            <!-- En-tête Modale -->
            <div class="p-6 border-b border-slate-100 flex items-start justify-between sticky top-0 bg-white z-10">
              <div>
                <span class="text-xs font-bold uppercase tracking-wider text-emerald-600">
                  {{ getMealTypeLabel(selectedMeal()!.type) }}
                </span>
                <h3 class="text-xl font-black text-slate-900 mt-0.5">
                  {{ selectedMeal()!.name }}
                </h3>
              </div>
              <button 
                type="button"
                (click)="closeMealDetails()"
                class="text-slate-400 hover:text-slate-700 text-2xl p-1 cursor-pointer">
                ✕
              </button>
            </div>

            <!-- Corps Modale -->
            <div class="p-6 space-y-6">
              
              <!-- Cartes des macros -->
              <div class="grid grid-cols-4 gap-3 text-center">
                <div class="p-3 rounded-2xl bg-slate-100">
                  <span class="block text-[11px] font-semibold text-slate-500 uppercase">Calories</span>
                  <span class="text-base font-extrabold text-slate-900">{{ selectedMeal()!.calories }} kcal</span>
                </div>
                <div class="p-3 rounded-2xl bg-sky-50">
                  <span class="block text-[11px] font-semibold text-sky-600 uppercase">Protéines</span>
                  <span class="text-base font-extrabold text-sky-900">{{ selectedMeal()!.protein }} g</span>
                </div>
                <div class="p-3 rounded-2xl bg-amber-50">
                  <span class="block text-[11px] font-semibold text-amber-600 uppercase">Glucides</span>
                  <span class="text-base font-extrabold text-amber-900">{{ selectedMeal()!.carbs }} g</span>
                </div>
                <div class="p-3 rounded-2xl bg-rose-50">
                  <span class="block text-[11px] font-semibold text-rose-600 uppercase">Lipides</span>
                  <span class="text-base font-extrabold text-rose-900">{{ selectedMeal()!.fats }} g</span>
                </div>
              </div>

              <!-- Liste des aliments & Remplacement avec l'IA -->
              <div>
                <div class="flex items-center justify-between mb-3">
                  <h4 class="text-sm font-bold text-slate-900 uppercase tracking-wider">
                    Aliments & Portions
                  </h4>
                  <span class="text-xs text-slate-400">Cliquez sur un aliment pour le remplacer avec l'IA</span>
                </div>

                <div class="space-y-2">
                  @for (food of selectedMeal()!.foods; track food.id) {
                    <div class="p-3.5 rounded-xl border border-slate-200 flex items-center justify-between hover:border-emerald-300 transition">
                      <div>
                        <div class="font-bold text-sm text-slate-800">{{ food.name }}</div>
                        <div class="text-xs text-slate-500 mt-0.5">
                          Quantité : <span class="font-semibold text-slate-700">{{ food.quantity }}</span> • {{ food.calories }} kcal
                          (P:{{ food.protein }}g, G:{{ food.carbs }}g, L:{{ food.fats }}g)
                        </div>
                      </div>

                      <button 
                        type="button"
                        (click)="replaceFoodWithAI(selectedMeal()!, food)"
                        [disabled]="isReplacingFood() === food.id"
                        class="px-3 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-bold transition cursor-pointer disabled:opacity-50">
                        @if (isReplacingFood() === food.id) {
                          <span>Remplacement...</span>
                        } @else {
                          <span>🔄 Remplacer par l'IA</span>
                        }
                      </button>
                    </div>
                  }
                </div>
              </div>

              <!-- Instructions de préparation -->
              <div>
                <h4 class="text-sm font-bold text-slate-900 uppercase tracking-wider mb-2">
                  👨‍🍳 Instructions de Préparation
                </h4>
                <div class="p-4 rounded-2xl bg-slate-50 border border-slate-200/70 text-sm text-slate-700 leading-relaxed whitespace-pre-line">
                  {{ selectedMeal()!.preparation || 'Cuire les ingrédients avec assaisonnement au goût et servir chaud.' }}
                </div>
              </div>

              <!-- Alternatives recommandées -->
              @if (selectedMeal()!.alternatives && selectedMeal()!.alternatives.length > 0) {
                <div>
                  <h4 class="text-sm font-bold text-slate-900 uppercase tracking-wider mb-2">
                    💡 Alternatives Suggérées
                  </h4>
                  <div class="flex flex-wrap gap-2">
                    @for (alt of selectedMeal()!.alternatives; track alt) {
                      <span class="px-3 py-1.5 rounded-xl bg-slate-100 text-slate-700 text-xs font-medium">
                        • {{ alt }}
                      </span>
                    }
                  </div>
                </div>
              }

            </div>

            <!-- Pied de Modale -->
            <div class="p-6 border-t border-slate-100 bg-slate-50/50 flex flex-wrap items-center justify-between gap-3 sticky bottom-0">
              <button 
                type="button"
                (click)="toggleMealCompleted(selectedMeal()!)"
                class="px-4 py-2.5 rounded-xl font-bold text-xs transition cursor-pointer"
                [class.bg-emerald-600]="!selectedMeal()!.isCompleted"
                [class.text-white]="!selectedMeal()!.isCompleted"
                [class.bg-slate-200]="selectedMeal()!.isCompleted"
                [class.text-slate-800]="selectedMeal()!.isCompleted">
                {{ selectedMeal()!.isCompleted ? '✓ Marquer comme non terminé' : '✓ Marquer repas comme terminé' }}
              </button>

              <button 
                type="button"
                (click)="regenerateMeal(selectedMeal()!)"
                [disabled]="isRegeneratingMeal() === selectedMeal()!.id"
                class="px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs transition cursor-pointer disabled:opacity-50">
                🔄 Régénérer entièrement ce repas avec l'IA
              </button>
            </div>

          </div>
        </div>
      }

    </div>
  `
})
export class MealPlanComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  public mealPlanService = inject(MealPlanService);
  public nutritionService = inject(NutritionService);
  public aiService = inject(AIService);

  public mealPlan = this.mealPlanService.currentPlan;
  public selectedDayIndex = signal<number>(0);
  public selectedMeal = signal<Meal | null>(null);
  public isRegeneratingMeal = signal<string | null>(null);
  public isReplacingFood = signal<string | null>(null);

  public steps = [
    { index: 1, title: 'Profil & Contraintes' },
    { index: 2, title: 'Calculs Métaboliques' },
    { index: 3, title: 'Architecture 7 jours' },
    { index: 4, title: 'Génération Repas IA' },
    { index: 5, title: 'Contrôle Allergènes' },
    { index: 6, title: 'Finalisation' }
  ];

  public currentDay = computed(() => {
    const plan = this.mealPlan();
    if (!plan || !plan.days || plan.days.length === 0) return null;
    const index = Math.min(this.selectedDayIndex(), plan.days.length - 1);
    return plan.days[index];
  });

  ngOnInit(): void {
    this.mealPlanService.loadCurrentPlan().subscribe({
      next: () => {},
      error: () => {}
    });

    // Vérifier si une vraie IA est configurée (réponse affichée à l'utilisateur)
    this.aiService.checkAIStatus().subscribe({ error: () => {} });

    // Détection du paramètre de requête pour auto-déclencher la génération
    this.route.queryParams.subscribe((params) => {
      if (params['generate'] === 'true') {
        this.regenerateAll();
        // Nettoyer l'URL
        this.router.navigate([], { replaceUrl: true });
      }
    });
  }

  regenerateAll(): void {
    this.aiService.generateMealPlan().subscribe({
      next: (newPlan) => {
        this.mealPlanService.currentPlan.set(newPlan);
        this.selectedDayIndex.set(0);
      }
    });
  }

  openMealDetails(meal: Meal): void {
    this.selectedMeal.set(meal);
  }

  closeMealDetails(): void {
    this.selectedMeal.set(null);
  }

  toggleMealCompleted(meal: Meal): void {
    const nextState = !meal.isCompleted;
    this.mealPlanService.toggleMealCompleted(meal.id, nextState).subscribe({
      next: (updated) => {
        if (this.selectedMeal()?.id === meal.id) {
          this.selectedMeal.set(updated);
        }
      }
    });
  }

  regenerateMeal(meal: Meal): void {
    this.isRegeneratingMeal.set(meal.id);
    this.aiService.regenerateMeal(meal.id).subscribe({
      next: (newMeal) => {
        this.isRegeneratingMeal.set(null);
        // Mettre à jour dans le plan actif
        const plan = this.mealPlan();
        if (plan) {
          const updatedDays = plan.days.map((day) => ({
            ...day,
            meals: day.meals.map((m) => m.id === meal.id ? newMeal : m)
          }));
          this.mealPlanService.currentPlan.set({ ...plan, days: updatedDays });
          if (this.selectedMeal()?.id === meal.id) {
            this.selectedMeal.set(newMeal);
          }
        }
      },
      error: () => {
        this.isRegeneratingMeal.set(null);
      }
    });
  }

  replaceFoodWithAI(meal: Meal, food: FoodItem): void {
    this.isReplacingFood.set(food.id);
    this.aiService.replaceFood(meal.id, food.id).subscribe({
      next: (updatedMeal) => {
        this.isReplacingFood.set(null);
        const plan = this.mealPlan();
        if (plan) {
          const updatedDays = plan.days.map((day) => ({
            ...day,
            meals: day.meals.map((m) => m.id === meal.id ? updatedMeal : m)
          }));
          this.mealPlanService.currentPlan.set({ ...plan, days: updatedDays });
          this.selectedMeal.set(updatedMeal);
        }
      },
      error: () => {
        this.isReplacingFood.set(null);
      }
    });
  }

  getMealEmoji(type: string): string {
    switch (type) {
      case 'breakfast': return '🍳';
      case 'lunch': return '🥗';
      case 'dinner': return '🍲';
      case 'morning_snack':
      case 'afternoon_snack':
      case 'evening_snack':
      default: return '🍎';
    }
  }

  getMealTypeLabel(type: string): string {
    switch (type) {
      case 'breakfast': return 'Petit-déjeuner';
      case 'lunch': return 'Déjeuner';
      case 'dinner': return 'Dîner';
      case 'morning_snack': return 'Collation matinale';
      case 'afternoon_snack': return 'Collation après-midi';
      case 'evening_snack': return 'Collation du soir';
      default: return 'Repas';
    }
  }
}
