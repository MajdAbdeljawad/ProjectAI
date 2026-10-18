import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError, finalize } from 'rxjs';
import { Meal, MealPlan } from '../models/nutrition.models';
import { NutritionService } from './nutrition.service';

export interface GenerationStepInfo {
  step: number;
  total: number;
  message: string;
}

export interface AIStatus {
  isRealAI: boolean;
  providerName: string;
  message: string;
}

export interface MealPlanWithMeta extends MealPlan {
  generatedBy?: {
    isRealAI: boolean;
    providerName: string;
    usedFallback: boolean;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AIService {
  private http = inject(HttpClient);
  private nutritionService = inject(NutritionService);
  private readonly baseUrl = '/nutrition';

  public isGenerating = signal<boolean>(false);
  public currentStep = signal<number>(0);
  public currentStepMessage = signal<string>('');
  public error = signal<string | null>(null);

  /** null = not yet checked, AIStatus object after first check */
  public aiStatus = signal<AIStatus | null>(null);

  private readonly STEPS = [
    'Analyse de votre profil et contraintes...',
    'Calcul métabolique de vos besoins énergétiques...',
    'Construction du prompt personnalisé...',
    'Génération des repas et recettes par l\'IA...',
    'Vérification de la conformité nutritionnelle et des allergènes...',
    'Finalisation et enregistrement de votre programme !'
  ];

  private stepTimer: any = null;

  /**
   * Vérifie si une vraie API IA générative est configurée côté backend.
   * La clé API ne transite jamais vers le frontend.
   */
  checkAIStatus(): Observable<AIStatus> {
    return this.http.get<AIStatus>(`${this.baseUrl}/ai-status`).pipe(
      tap(status => this.aiStatus.set(status))
    );
  }

  /**
   * Lance la génération complète d'un programme de 7 jours.
   * Les repas proviennent exclusivement du backend (OpenAI/Gemini ou moteur algorithmique).
   */
  generateMealPlan(): Observable<MealPlanWithMeta> {
    this.isGenerating.set(true);
    this.error.set(null);
    this.currentStep.set(1);
    this.currentStepMessage.set(this.STEPS[0]);

    // Animation réaliste d'avancement des étapes
    this.startStepAnimation();

    const profile = this.nutritionService.currentProfile() ?? {};

    return this.http.post<MealPlanWithMeta>(`${this.baseUrl}/meal-plan/generate`, profile).pipe(
      tap((plan) => {
        this.currentStep.set(this.STEPS.length);
        this.currentStepMessage.set(this.STEPS[this.STEPS.length - 1]);
        // Mettre à jour le statut IA à partir des métadonnées de la réponse
        if (plan.generatedBy) {
          this.aiStatus.set({
            isRealAI: plan.generatedBy.isRealAI,
            providerName: plan.generatedBy.providerName,
            message: plan.generatedBy.isRealAI
              ? `Repas générés par ${plan.generatedBy.providerName}`
              : 'Mode développement — Configuration API IA requise pour une génération par IA réelle.'
          });
        }
        return plan;
      }),
      catchError((err) => {
        const msg = err.error?.error || err.message || 'Échec de la génération.';
        this.error.set(msg);
        return throwError(() => new Error(msg));
      }),
      finalize(() => {
        this.stopStepAnimation();
        setTimeout(() => {
          this.isGenerating.set(false);
          this.currentStep.set(0);
        }, 800);
      })
    );
  }

  /**
   * Régénère un repas individuel via le backend (IA ou fallback)
   */
  regenerateMeal(mealId: string): Observable<Meal> {
    return this.http.post<Meal>(`${this.baseUrl}/meal/${mealId}/regenerate`, {});
  }

  /**
   * Remplace un aliment d'un repas par un équivalent nutritionnel via le backend
   */
  replaceFood(mealId: string, foodId: string): Observable<Meal> {
    return this.http.post<Meal>(`${this.baseUrl}/meal/${mealId}/replace-food`, { foodId });
  }

  private startStepAnimation(): void {
    let index = 0;
    this.stopStepAnimation();
    this.stepTimer = setInterval(() => {
      if (index < this.STEPS.length - 1) {
        index++;
        this.currentStep.set(index + 1);
        this.currentStepMessage.set(this.STEPS[index]);
      }
    }, 1400);
  }

  private stopStepAnimation(): void {
    if (this.stepTimer) {
      clearInterval(this.stepTimer);
      this.stepTimer = null;
    }
  }
}
