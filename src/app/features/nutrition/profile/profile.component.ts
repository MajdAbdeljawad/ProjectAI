import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NutritionService } from '../../../core/services/nutrition.service';
import { NutritionCalculatorService } from '../../../core/services/nutrition-calculator.service';
import { AIService } from '../../../core/services/ai.service';
import { 
  DietType, 
  NutritionGoal, 
  NutritionProfile, 
  Sex, 
  SportsLevel, 
  WorkoutType,
  BudgetLevel,
  CookingTimePreference 
} from '../../../core/models/nutrition.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="max-w-6xl mx-auto px-4 py-8">
      <!-- En-tête de la page -->
      <div class="flex flex-col md:flex-row md:items-center justify-between pb-6 mb-8 border-b border-slate-200">
        <div>
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-2xl bg-emerald-500/10 flex items-center justify-center text-emerald-600 font-bold text-xl">
              🥗
            </div>
            <div>
              <h1 class="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight">
                Profil Nutritionnel & Sportif
              </h1>
              <p class="text-sm text-slate-500 mt-0.5">
                Vos données sont sauvegardées en base et utilisées pour la génération personnalisée par l'IA.
              </p>
            </div>
          </div>
        </div>

        <!-- Boutons d'action Consultation / Édition -->
        <div class="mt-4 md:mt-0 flex items-center gap-3">
          @if (!isEditing()) {
            <button 
              type="button"
              (click)="enableEditMode()"
              class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm shadow-sm transition-all transform active:scale-95 cursor-pointer">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
              </svg>
              Modifier mon profil
            </button>
            <button 
              type="button"
              (click)="generateWithCurrentProfile()"
              [disabled]="aiService.isGenerating()"
              class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm shadow-sm transition-all transform active:scale-95 cursor-pointer disabled:opacity-50">
              <span>⚡</span>
              Générer mon programme IA
            </button>
          } @else {
            <button 
              type="button"
              (click)="cancelEdit()"
              class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-medium text-sm transition-all cursor-pointer">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
              Annuler
            </button>
            <button 
              type="button"
              (click)="saveProfile()"
              [disabled]="nutritionService.isLoading() || profileForm.invalid"
              class="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm shadow-md transition-all transform active:scale-95 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
              @if (nutritionService.isLoading()) {
                <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                Enregistrement...
              } @else {
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                Enregistrer les modifications
              }
            </button>
          }
        </div>
      </div>

      <!-- Messages d'état (Succès / Erreur) -->
      @if (nutritionService.successMessage()) {
        <div class="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-center justify-between animate-fadeIn">
          <div class="flex items-center gap-3">
            <span class="text-xl">✅</span>
            <span class="font-medium text-sm">{{ nutritionService.successMessage() }}</span>
          </div>
          <button (click)="nutritionService.clearMessages()" class="text-emerald-600 hover:text-emerald-900 cursor-pointer">✕</button>
        </div>
      }

      @if (nutritionService.errorMessage()) {
        <div class="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="text-xl">⚠️</span>
            <span class="font-medium text-sm">{{ nutritionService.errorMessage() }}</span>
          </div>
          <button (click)="nutritionService.clearMessages()" class="text-rose-600 hover:text-rose-900 cursor-pointer">✕</button>
        </div>
      }

      <!-- Grille Principale : Formulaire / Vue + Carte Calculs Nutritionnels -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        <!-- Colonne Gauche : Formulaire (2 colonnes sur écran large) -->
        <div class="lg:col-span-2 space-y-8">
          <form [formGroup]="profileForm">
            
            <!-- SECTION 1 : INFORMATIONS PERSONNELLES -->
            <div class="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-6">
              <div class="flex items-center gap-3 pb-4 mb-5 border-b border-slate-100">
                <span class="text-xl">👤</span>
                <h2 class="text-lg font-bold text-slate-900">1. Informations Personnelles</h2>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <!-- Nom complet -->
                <div class="sm:col-span-2">
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Nom et Prénom
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('fullName')?.value || 'Non renseigné' }}
                    </p>
                  } @else {
                    <input 
                      type="text" 
                      formControlName="fullName"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition"
                      placeholder="Ex: Alexandre Laurent" />
                  }
                </div>

                <!-- Âge -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Âge (années)
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('age')?.value }} ans
                    </p>
                  } @else {
                    <input 
                      type="number" 
                      formControlName="age"
                      min="12" max="100"
                      (input)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition" />
                  }
                </div>

                <!-- Sexe -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Sexe biologique
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60 capitalize">
                      {{ profileForm.get('sex')?.value }}
                    </p>
                  } @else {
                    <select 
                      formControlName="sex"
                      (change)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition cursor-pointer">
                      <option value="homme">Homme</option>
                      <option value="femme">Femme</option>
                      <option value="autre">Autre</option>
                    </select>
                  }
                </div>

                <!-- Taille -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Taille (cm)
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('height')?.value }} cm
                    </p>
                  } @else {
                    <input 
                      type="number" 
                      formControlName="height"
                      min="100" max="230"
                      (input)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition" />
                  }
                </div>

                <!-- Poids -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Poids (kg)
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('weight')?.value }} kg
                    </p>
                  } @else {
                    <input 
                      type="number" 
                      step="0.5"
                      formControlName="weight"
                      min="35" max="250"
                      (input)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition" />
                  }
                </div>
              </div>
            </div>

            <!-- SECTION 2 : OBJECTIF PRINCIPAL -->
            <div class="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-6">
              <div class="flex items-center gap-3 pb-4 mb-5 border-b border-slate-100">
                <span class="text-xl">🎯</span>
                <h2 class="text-lg font-bold text-slate-900">2. Objectif Nutritionnel</h2>
              </div>

              <div>
                <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
                  Objectif recherché
                </label>
                @if (!isEditing()) {
                  <div class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-50 text-emerald-800 border border-emerald-200 font-bold text-base">
                    <span>{{ getGoalLabel(profileForm.get('goal')?.value) }}</span>
                  </div>
                } @else {
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    @for (opt of goalsList; track opt.id) {
                      <label 
                        class="flex items-center gap-3 p-3.5 rounded-xl border cursor-pointer transition-all"
                        [class.border-emerald-500]="profileForm.get('goal')?.value === opt.id"
                        [class.bg-emerald-50/50]="profileForm.get('goal')?.value === opt.id"
                        [class.border-slate-200]="profileForm.get('goal')?.value !== opt.id">
                        <input 
                          type="radio" 
                          formControlName="goal" 
                          [value]="opt.id"
                          (change)="onMetricChanged()"
                          class="text-emerald-600 focus:ring-emerald-500 cursor-pointer" />
                        <div>
                          <div class="text-sm font-bold text-slate-900">{{ opt.label }}</div>
                          <div class="text-xs text-slate-500">{{ opt.desc }}</div>
                        </div>
                      </label>
                    }
                  </div>
                }
              </div>
            </div>

            <!-- SECTION 3 : ACTIVITÉ PHYSIQUE & SPORT -->
            <div class="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-6">
              <div class="flex items-center gap-3 pb-4 mb-5 border-b border-slate-100">
                <span class="text-xl">🏋️</span>
                <h2 class="text-lg font-bold text-slate-900">3. Informations Sportives</h2>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-3 gap-5">
                <!-- Niveau sportif -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Niveau sportif
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60 capitalize">
                      {{ profileForm.get('sportsLevel')?.value }}
                    </p>
                  } @else {
                    <select 
                      formControlName="sportsLevel"
                      (change)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition cursor-pointer">
                      <option value="débutant">Débutant (0 - 1 an)</option>
                      <option value="intermédiaire">Intermédiaire (1 - 3 ans)</option>
                      <option value="avancé">Avancé (3+ ans)</option>
                    </select>
                  }
                </div>

                <!-- Nombre de séances par semaine -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Séances par semaine
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('sessionsPerWeek')?.value }} séances
                    </p>
                  } @else {
                    <select 
                      formControlName="sessionsPerWeek"
                      (change)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition cursor-pointer">
                      <option [value]="1">1 séance / semaine</option>
                      <option [value]="2">2 séances / semaine</option>
                      <option [value]="3">3 séances / semaine</option>
                      <option [value]="4">4 séances / semaine</option>
                      <option [value]="5">5 séances / semaine</option>
                      <option [value]="6">6 séances / semaine</option>
                      <option [value]="7">7 séances / semaine</option>
                    </select>
                  }
                </div>

                <!-- Type d'entraînement -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Type d'entraînement
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60 capitalize">
                      {{ profileForm.get('workoutType')?.value }}
                    </p>
                  } @else {
                    <select 
                      formControlName="workoutType"
                      (change)="onMetricChanged()"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition cursor-pointer">
                      <option value="musculation">Musculation / Bodybuilding</option>
                      <option value="cardio">Cardio-training</option>
                      <option value="running">Running / Course</option>
                      <option value="football">Football / Sport collectif</option>
                      <option value="CrossFit">CrossFit / HIIT</option>
                      <option value="autre">Autre activité</option>
                    </select>
                  }
                </div>
              </div>
            </div>

            <!-- SECTION 4 : PRÉFÉRENCES ALIMENTAIRES & ALLERGIES -->
            <div class="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-6">
              <div class="flex items-center gap-3 pb-4 mb-5 border-b border-slate-100">
                <span class="text-xl">🥑</span>
                <h2 class="text-lg font-bold text-slate-900">4. Préférences, Allergies & Restrictions</h2>
              </div>

              <!-- Régime alimentaire -->
              <div class="mb-5">
                <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                  Régime alimentaire
                </label>
                @if (!isEditing()) {
                  <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60 capitalize">
                    {{ getDietLabel(profileForm.get('diet')?.value) }}
                  </p>
                } @else {
                  <select 
                    formControlName="diet"
                    (change)="onMetricChanged()"
                    class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 text-slate-800 font-medium text-sm transition cursor-pointer">
                    <option value="omnivore">Omnivore (Mange de tout)</option>
                    <option value="vegetarien">Végétarien (Sans viande ni poisson)</option>
                    <option value="vegan">Végétalien / Vegan (100% végétal)</option>
                    <option value="pesco_vegetarien">Pesco-végétarien (Poisson accepté)</option>
                    <option value="cetogene">Cétogène / Keto (Très faible en glucides)</option>
                    <option value="sans_gluten">Sans gluten strict</option>
                    <option value="sans_lactose">Sans lactose</option>
                  </select>
                }
              </div>

              <!-- Allergies -->
              <div class="mb-5">
                <label class="block text-xs font-semibold uppercase tracking-wider text-rose-600 mb-1.5">
                  ⚠️ Allergies strictes (Exclues impérativement par l'IA)
                </label>
                @if (!isEditing()) {
                  <div class="flex flex-wrap gap-2 pt-1">
                    @if (allergiesList().length === 0) {
                      <span class="text-sm text-slate-400 italic">Aucune allergie signalée</span>
                    } @else {
                      @for (item of allergiesList(); track item) {
                        <span class="px-3 py-1 rounded-lg bg-rose-50 text-rose-700 border border-rose-200 text-xs font-bold">
                          🚫 {{ item }}
                        </span>
                      }
                    }
                  </div>
                } @else {
                  <div class="space-y-2">
                    <div class="flex gap-2">
                      <input 
                        type="text" 
                        [(ngModel)]="newAllergyInput" 
                        [ngModelOptions]="{standalone: true}"
                        placeholder="Ex: Arachides, Fruits de mer, Gluten..."
                        (keydown.enter)="$event.preventDefault(); addTag('allergy')"
                        class="flex-1 px-3.5 py-2 rounded-xl border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500" />
                      <button 
                        type="button" 
                        (click)="addTag('allergy')" 
                        class="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white font-medium text-sm rounded-xl cursor-pointer">
                        Ajouter
                      </button>
                    </div>
                    <div class="flex flex-wrap gap-2 pt-1">
                      @for (item of allergiesList(); track item) {
                        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-rose-50 text-rose-700 border border-rose-200 text-xs font-semibold">
                          {{ item }}
                          <button type="button" (click)="removeTag('allergy', item)" class="text-rose-500 hover:text-rose-900 font-bold ml-1">✕</button>
                        </span>
                      }
                    </div>
                  </div>
                }
              </div>

              <!-- Aliments préférés et détestés -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <!-- Aliments préférés -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-emerald-600 mb-1.5">
                    Aliments préférés
                  </label>
                  @if (!isEditing()) {
                    <div class="flex flex-wrap gap-1.5">
                      @for (item of preferredFoodsList(); track item) {
                        <span class="px-2.5 py-0.5 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-medium">
                          ✓ {{ item }}
                        </span>
                      }
                    </div>
                  } @else {
                    <div class="space-y-2">
                      <div class="flex gap-2">
                        <input 
                          type="text" 
                          [(ngModel)]="newPreferredInput"
                          [ngModelOptions]="{standalone: true}"
                          placeholder="Ex: Saumon, Avoine..."
                          (keydown.enter)="$event.preventDefault(); addTag('preferred')"
                          class="flex-1 px-3 py-1.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:border-emerald-500" />
                        <button type="button" (click)="addTag('preferred')" class="px-3 py-1.5 bg-emerald-600 text-white rounded-xl text-xs font-semibold cursor-pointer">
                          +
                        </button>
                      </div>
                      <div class="flex flex-wrap gap-1.5">
                        @for (item of preferredFoodsList(); track item) {
                          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-700 text-xs">
                            {{ item }}
                            <button type="button" (click)="removeTag('preferred', item)" class="hover:text-emerald-900 font-bold">✕</button>
                          </span>
                        }
                      </div>
                    </div>
                  }
                </div>

                <!-- Aliments détestés -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Aliments détestés
                  </label>
                  @if (!isEditing()) {
                    <div class="flex flex-wrap gap-1.5">
                      @for (item of dislikedFoodsList(); track item) {
                        <span class="px-2.5 py-0.5 rounded-md bg-slate-100 text-slate-700 border border-slate-200 text-xs font-medium">
                          ✗ {{ item }}
                        </span>
                      }
                    </div>
                  } @else {
                    <div class="space-y-2">
                      <div class="flex gap-2">
                        <input 
                          type="text" 
                          [(ngModel)]="newDislikedInput"
                          [ngModelOptions]="{standalone: true}"
                          placeholder="Ex: Céleri, Foie..."
                          (keydown.enter)="$event.preventDefault(); addTag('disliked')"
                          class="flex-1 px-3 py-1.5 rounded-xl border border-slate-300 text-sm focus:outline-none focus:border-slate-500" />
                        <button type="button" (click)="addTag('disliked')" class="px-3 py-1.5 bg-slate-700 text-white rounded-xl text-xs font-semibold cursor-pointer">
                          +
                        </button>
                      </div>
                      <div class="flex flex-wrap gap-1.5">
                        @for (item of dislikedFoodsList(); track item) {
                          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 text-xs">
                            {{ item }}
                            <button type="button" (click)="removeTag('disliked', item)" class="hover:text-slate-900 font-bold">✕</button>
                          </span>
                        }
                      </div>
                    </div>
                  }
                </div>
              </div>
            </div>

            <!-- SECTION 5 : MODE DE VIE & RYTHME -->
            <div class="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-6">
              <div class="flex items-center gap-3 pb-4 mb-5 border-b border-slate-100">
                <span class="text-xl">⏰</span>
                <h2 class="text-lg font-bold text-slate-900">5. Mode de Vie & Rythme Journalier</h2>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-5">
                <!-- Nombre de repas -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Repas par jour
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('mealsPerDay')?.value }} repas
                    </p>
                  } @else {
                    <select 
                      formControlName="mealsPerDay"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-emerald-500 text-slate-800 font-medium text-sm cursor-pointer">
                      <option [value]="2">2 repas (Jeûne intermittent)</option>
                      <option [value]="3">3 repas traditionnels</option>
                      <option [value]="4">4 repas (3 + 1 collation)</option>
                      <option [value]="5">5 repas (3 + 2 collations)</option>
                      <option [value]="6">6 repas (Prise de masse fractionnée)</option>
                    </select>
                  }
                </div>

                <!-- Heure de réveil -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Heure de réveil
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('wakeUpTime')?.value }}
                    </p>
                  } @else {
                    <input 
                      type="time" 
                      formControlName="wakeUpTime"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-emerald-500 text-slate-800 font-medium text-sm" />
                  }
                </div>

                <!-- Heure du coucher -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Heure du coucher
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60">
                      {{ profileForm.get('sleepTime')?.value }}
                    </p>
                  } @else {
                    <input 
                      type="time" 
                      formControlName="sleepTime"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-emerald-500 text-slate-800 font-medium text-sm" />
                  }
                </div>

                <!-- Budget -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Budget alimentaire
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60 capitalize">
                      {{ profileForm.get('budget')?.value }}
                    </p>
                  } @else {
                    <select 
                      formControlName="budget"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-emerald-500 text-slate-800 font-medium text-sm cursor-pointer">
                      <option value="economique">Économique (Étudiant / serré)</option>
                      <option value="modere">Modéré (Moyen équilibré)</option>
                      <option value="confortable">Confortable (Produits frais / bio)</option>
                      <option value="eleve">Élevé (Premium sans contrainte)</option>
                    </select>
                  }
                </div>

                <!-- Temps disponible pour cuisiner -->
                <div>
                  <label class="block text-xs font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                    Temps de cuisine
                  </label>
                  @if (!isEditing()) {
                    <p class="text-base font-semibold text-slate-800 bg-slate-50 px-3.5 py-2.5 rounded-xl border border-slate-200/60 capitalize">
                      {{ profileForm.get('cookingTime')?.value === 'rapide' ? 'Rapide (<15 min)' : (profileForm.get('cookingTime')?.value === 'moyen' ? 'Moyen (15-30 min)' : 'Long (>30 min)') }}
                    </p>
                  } @else {
                    <select 
                      formControlName="cookingTime"
                      class="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-emerald-500 text-slate-800 font-medium text-sm cursor-pointer">
                      <option value="rapide">Express / Rapide (&lt; 15 min)</option>
                      <option value="moyen">Moyen (15 à 30 min)</option>
                      <option value="long">Grand cuisinier (&gt; 30 min)</option>
                    </select>
                  }
                </div>
              </div>
            </div>

          </form>
        </div>

        <!-- Colonne Droite : Carte Calculs Scientifiques & Aperçu Dynamique -->
        <div class="space-y-6">
          <div class="sticky top-6 bg-gradient-to-br from-slate-900 via-slate-800 to-emerald-950 text-white rounded-3xl p-6 shadow-xl border border-slate-700/60">
            <div class="flex items-center justify-between pb-4 border-b border-slate-700/60">
              <span class="text-xs uppercase tracking-widest font-extrabold text-emerald-400">
                Calculs Nutritionnels
              </span>
              <span class="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-semibold">
                Mifflin-St Jeor
              </span>
            </div>

            <!-- Calories cibles principales -->
            <div class="my-6 text-center">
              <div class="text-4xl md:text-5xl font-black tracking-tight text-white">
                {{ liveCalculations().targetCalories }}
                <span class="text-lg font-bold text-emerald-400">kcal / jour</span>
              </div>
              <p class="text-xs text-slate-400 mt-1">
                Objectif : {{ getGoalLabel(profileForm.get('goal')?.value) }}
              </p>
            </div>

            <!-- Métabolisme & Dépense -->
            <div class="grid grid-cols-2 gap-3 mb-6">
              <div class="p-3 rounded-2xl bg-white/5 border border-white/10 text-center">
                <span class="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Métabolisme Base (BMR)</span>
                <span class="text-lg font-bold text-white">{{ liveCalculations().bmr }} kcal</span>
              </div>
              <div class="p-3 rounded-2xl bg-white/5 border border-white/10 text-center">
                <span class="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Dépense Totale (TDEE)</span>
                <span class="text-lg font-bold text-emerald-300">{{ liveCalculations().tdee }} kcal</span>
              </div>
            </div>

            <!-- Répartition des Macronutriments Cibles -->
            <div class="space-y-3 pt-2">
              <div class="text-xs font-bold text-slate-300 uppercase tracking-wider">Répartition Cible :</div>
              
              <!-- Protéines -->
              <div class="p-3 rounded-2xl bg-white/5 border border-white/10">
                <div class="flex justify-between items-center text-sm font-semibold mb-1">
                  <span class="text-sky-400 flex items-center gap-1.5">🥩 Protéines</span>
                  <span class="text-white">{{ liveCalculations().macros.protein }} g <span class="text-xs text-slate-400 font-normal">({{ liveCalculations().macros.protein * 4 }} kcal)</span></span>
                </div>
                <div class="w-full bg-slate-700/50 rounded-full h-1.5">
                  <div class="bg-sky-400 h-1.5 rounded-full" [style.width]="getMacroPercent('p') + '%'"></div>
                </div>
              </div>

              <!-- Glucides -->
              <div class="p-3 rounded-2xl bg-white/5 border border-white/10">
                <div class="flex justify-between items-center text-sm font-semibold mb-1">
                  <span class="text-amber-400 flex items-center gap-1.5">🍚 Glucides</span>
                  <span class="text-white">{{ liveCalculations().macros.carbs }} g <span class="text-xs text-slate-400 font-normal">({{ liveCalculations().macros.carbs * 4 }} kcal)</span></span>
                </div>
                <div class="w-full bg-slate-700/50 rounded-full h-1.5">
                  <div class="bg-amber-400 h-1.5 rounded-full" [style.width]="getMacroPercent('c') + '%'"></div>
                </div>
              </div>

              <!-- Lipides -->
              <div class="p-3 rounded-2xl bg-white/5 border border-white/10">
                <div class="flex justify-between items-center text-sm font-semibold mb-1">
                  <span class="text-rose-400 flex items-center gap-1.5">🥑 Lipides</span>
                  <span class="text-white">{{ liveCalculations().macros.fats }} g <span class="text-xs text-slate-400 font-normal">({{ liveCalculations().macros.fats * 9 }} kcal)</span></span>
                </div>
                <div class="w-full bg-slate-700/50 rounded-full h-1.5">
                  <div class="bg-rose-400 h-1.5 rounded-full" [style.width]="getMacroPercent('f') + '%'"></div>
                </div>
              </div>
            </div>

            <!-- Disclaimer médical -->
            <p class="text-[11px] text-slate-400 mt-6 pt-4 border-t border-slate-700/60 leading-relaxed italic">
              {{ medicalDisclaimer }}
            </p>

            <!-- Bouton déclencheur IA -->
            <div class="mt-6">
              <button 
                type="button"
                (click)="generateWithCurrentProfile()"
                [disabled]="aiService.isGenerating()"
                class="w-full py-3.5 px-4 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-extrabold text-sm shadow-lg transition-all transform active:scale-95 cursor-pointer disabled:opacity-50">
                🚀 Générer mon programme avec ces valeurs
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  `
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  public nutritionService = inject(NutritionService);
  public calculatorService = inject(NutritionCalculatorService);
  public aiService = inject(AIService);

  public isEditing = signal<boolean>(false);
  public allergiesList = signal<string[]>([]);
  public preferredFoodsList = signal<string[]>([]);
  public dislikedFoodsList = signal<string[]>([]);

  public newAllergyInput = '';
  public newPreferredInput = '';
  public newDislikedInput = '';

  public medicalDisclaimer = NutritionCalculatorService.MEDICAL_DISCLAIMER;

  public goalsList = [
    { id: 'perte_de_poids', label: 'Perte de poids', desc: 'Déficit contrôlé pour préserver le muscle' },
    { id: 'prise_de_masse', label: 'Prise de masse musculaire', desc: 'Surplus calorique propre et riche en protéines' },
    { id: 'maintien', label: 'Maintien & Forme', desc: 'Équilibre parfait dépenses / apports' },
    { id: 'performance', label: 'Amélioration des performances', desc: 'Carburant optimal pour les entraînements intensifs' },
    { id: 'reequilibrage', label: 'Rééquilibrage alimentaire', desc: 'Alimentation saine et habitudes durables' }
  ];

  public profileForm: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    age: [25, [Validators.required, Validators.min(14), Validators.max(100)]],
    sex: ['homme', Validators.required],
    height: [175, [Validators.required, Validators.min(120), Validators.max(240)]],
    weight: [70, [Validators.required, Validators.min(40), Validators.max(250)]],
    sportsLevel: ['intermédiaire', Validators.required],
    sessionsPerWeek: [3, Validators.required],
    workoutType: ['musculation', Validators.required],
    goal: ['prise_de_masse', Validators.required],
    diet: ['omnivore', Validators.required],
    mealsPerDay: [3, Validators.required],
    wakeUpTime: ['07:00', Validators.required],
    sleepTime: ['23:00', Validators.required],
    budget: ['modere', Validators.required],
    cookingTime: ['moyen', Validators.required]
  });

  // Calculs dynamiques en temps réel pendant que l'utilisateur manipule les champs
  public liveCalculations = computed(() => {
    const raw = this.profileForm.value;
    return this.calculatorService.calculateFullNeeds({
      age: Number(raw.age) || 25,
      weight: Number(raw.weight) || 70,
      height: Number(raw.height) || 175,
      sex: (raw.sex as Sex) || 'homme',
      sportsLevel: (raw.sportsLevel as SportsLevel) || 'intermédiaire',
      sessionsPerWeek: Number(raw.sessionsPerWeek) || 3,
      workoutType: (raw.workoutType as WorkoutType) || 'musculation',
      goal: (raw.goal as NutritionGoal) || 'maintien',
      diet: (raw.diet as DietType) || 'omnivore'
    });
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.nutritionService.loadProfile().subscribe({
      next: (profile) => this.populateForm(profile),
      error: () => {
        // En cas d'absence initiale, utiliser valeurs par défaut
        this.populateForm({
          id: 'temp',
          userId: 'default_user_1',
          fullName: 'Alexandre Laurent',
          age: 26,
          sex: 'homme',
          height: 180,
          weight: 78,
          sportsLevel: 'intermédiaire',
          sessionsPerWeek: 4,
          workoutType: 'musculation',
          goal: 'prise_de_masse',
          diet: 'omnivore',
          preferredFoods: ['Poulet', 'Riz basmati', 'Oeufs', 'Avocat'],
          dislikedFoods: ['Chou de Bruxelles'],
          allergies: [],
          intolerances: [],
          dietaryRestrictions: [],
          mealsPerDay: 4,
          wakeUpTime: '07:00',
          sleepTime: '23:00',
          budget: 'modere',
          cookingTime: 'moyen',
          bmr: 1790,
          tdee: 2775,
          targetCalories: 3125,
          targetProtein: 164,
          targetCarbs: 390,
          targetFats: 82
        });
      }
    });
  }

  populateForm(profile: NutritionProfile): void {
    this.profileForm.patchValue({
      fullName: profile.fullName,
      age: profile.age,
      sex: profile.sex,
      height: profile.height,
      weight: profile.weight,
      sportsLevel: profile.sportsLevel,
      sessionsPerWeek: profile.sessionsPerWeek,
      workoutType: profile.workoutType,
      goal: profile.goal,
      diet: profile.diet,
      mealsPerDay: profile.mealsPerDay,
      wakeUpTime: profile.wakeUpTime,
      sleepTime: profile.sleepTime,
      budget: profile.budget,
      cookingTime: profile.cookingTime
    });

    this.allergiesList.set([...(profile.allergies || [])]);
    this.preferredFoodsList.set([...(profile.preferredFoods || [])]);
    this.dislikedFoodsList.set([...(profile.dislikedFoods || [])]);
  }

  enableEditMode(): void {
    this.nutritionService.clearMessages();
    this.isEditing.set(true);
  }

  cancelEdit(): void {
    const current = this.nutritionService.currentProfile();
    if (current) {
      this.populateForm(current);
    }
    this.nutritionService.clearMessages();
    this.isEditing.set(false);
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const raw = this.profileForm.value;
    const calc = this.liveCalculations();

    const profilePayload: Partial<NutritionProfile> = {
      userId: this.nutritionService.currentProfile()?.userId || 'default_user_1',
      ...raw,
      allergies: this.allergiesList(),
      preferredFoods: this.preferredFoodsList(),
      dislikedFoods: this.dislikedFoodsList(),
      intolerances: [],
      dietaryRestrictions: [],
      bmr: calc.bmr,
      tdee: calc.tdee,
      targetCalories: calc.targetCalories,
      targetProtein: calc.macros.protein,
      targetCarbs: calc.macros.carbs,
      targetFats: calc.macros.fats
    };

    this.nutritionService.saveProfile(profilePayload).subscribe({
      next: () => {
        this.isEditing.set(false);
      }
    });
  }

  generateWithCurrentProfile(): void {
    // Si des modifications étaient en cours, sauvegarder d'abord
    if (this.isEditing()) {
      this.saveProfile();
    }
    this.router.navigate(['/meal-plan'], { queryParams: { generate: 'true' } });
  }

  onMetricChanged(): void {
    // Computed signal updates automatically
  }

  addTag(type: 'allergy' | 'preferred' | 'disliked'): void {
    if (type === 'allergy' && this.newAllergyInput.trim()) {
      this.allergiesList.set([...this.allergiesList(), this.newAllergyInput.trim()]);
      this.newAllergyInput = '';
    } else if (type === 'preferred' && this.newPreferredInput.trim()) {
      this.preferredFoodsList.set([...this.preferredFoodsList(), this.newPreferredInput.trim()]);
      this.newPreferredInput = '';
    } else if (type === 'disliked' && this.newDislikedInput.trim()) {
      this.dislikedFoodsList.set([...this.dislikedFoodsList(), this.newDislikedInput.trim()]);
      this.newDislikedInput = '';
    }
  }

  removeTag(type: 'allergy' | 'preferred' | 'disliked', item: string): void {
    if (type === 'allergy') {
      this.allergiesList.set(this.allergiesList().filter(x => x !== item));
    } else if (type === 'preferred') {
      this.preferredFoodsList.set(this.preferredFoodsList().filter(x => x !== item));
    } else if (type === 'disliked') {
      this.dislikedFoodsList.set(this.dislikedFoodsList().filter(x => x !== item));
    }
  }

  getGoalLabel(goal: string): string {
    const found = this.goalsList.find(g => g.id === goal);
    return found ? found.label : goal;
  }

  getDietLabel(diet: string): string {
    const map: Record<string, string> = {
      omnivore: 'Omnivore',
      vegetarien: 'Végétarien',
      vegan: 'Végétalien / Vegan',
      pesco_vegetarien: 'Pesco-végétarien',
      cetogene: 'Cétogène / Keto',
      sans_gluten: 'Sans gluten',
      sans_lactose: 'Sans lactose'
    };
    return map[diet] || diet;
  }

  getMacroPercent(type: 'p' | 'c' | 'f'): number {
    const cal = this.liveCalculations().targetCalories || 2000;
    const macros = this.liveCalculations().macros;
    if (type === 'p') return Math.round(((macros.protein * 4) / cal) * 100);
    if (type === 'c') return Math.round(((macros.carbs * 4) / cal) * 100);
    if (type === 'f') return Math.round(((macros.fats * 9) / cal) * 100);
    return 33;
  }
}
