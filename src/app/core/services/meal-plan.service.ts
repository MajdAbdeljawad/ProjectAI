import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { Meal, MealPlan } from '../models/nutrition.models';

export interface HistoryItem {
  id: string;
  title: string;
  createdAt: string;
  isActive: boolean;
  targetCalories: number;
  durationDays: number;
}

@Injectable({
  providedIn: 'root'
})
export class MealPlanService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/nutrition';

  public currentPlan = signal<MealPlan | null>(null);
  public history = signal<HistoryItem[]>([]);
  public isLoading = signal<boolean>(false);
  public error = signal<string | null>(null);

  /**
   * Charge le programme actif de l'utilisateur
   */
  loadCurrentPlan(): Observable<MealPlan> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<MealPlan>(`${this.baseUrl}/meal-plan/current`).pipe(
      tap((plan) => {
        this.currentPlan.set(plan);
        this.isLoading.set(false);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        if (err.status !== 404) {
          this.error.set(err.error?.error || 'Erreur lors du chargement du programme alimentaire.');
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Récupère l'historique des programmes créés
   */
  loadHistory(): Observable<HistoryItem[]> {
    return this.http.get<HistoryItem[]>(`${this.baseUrl}/meal-plan/history`).pipe(
      tap((items) => this.history.set(items)),
      catchError((err) => {
        console.error('Erreur chargement historique:', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Active un programme de l'historique
   */
  activatePlan(id: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.baseUrl}/meal-plan/${id}/activate`, {}).pipe(
      tap(() => {
        this.loadCurrentPlan().subscribe();
        this.loadHistory().subscribe();
      })
    );
  }

  /**
   * Supprime un programme de l'historique
   */
  deletePlan(id: string): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.baseUrl}/meal-plan/${id}`).pipe(
      tap(() => {
        this.loadHistory().subscribe();
        if (this.currentPlan()?.id === id) {
          this.currentPlan.set(null);
        }
      })
    );
  }

  /**
   * Bascule l'état d'achèvement d'un repas (ex: "Repas terminé")
   */
  toggleMealCompleted(mealId: string, isCompleted: boolean): Observable<Meal> {
    return this.http.patch<Meal>(`${this.baseUrl}/meal/${mealId}/toggle-complete`, { isCompleted }).pipe(
      tap((updatedMeal) => {
        // Mettre à jour localement dans le signal courant sans recharger tout
        const plan = this.currentPlan();
        if (!plan) return;

        const updatedDays = plan.days.map((day) => ({
          ...day,
          meals: day.meals.map((m) => m.id === mealId ? { ...m, isCompleted: updatedMeal.isCompleted } : m)
        }));

        this.currentPlan.set({ ...plan, days: updatedDays });
      })
    );
  }
}
