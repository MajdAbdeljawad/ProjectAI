import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { NutritionProfile } from '../models/nutrition.models';

@Injectable({
  providedIn: 'root'
})
export class NutritionService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/nutrition';

  public currentProfile = signal<NutritionProfile | null>(null);
  public isLoading = signal<boolean>(false);
  public errorMessage = signal<string | null>(null);
  public successMessage = signal<string | null>(null);

  /**
   * Récupère le profil nutritionnel depuis le backend
   */
  loadProfile(): Observable<NutritionProfile> {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    return this.http.get<NutritionProfile>(`${this.baseUrl}/profile`).pipe(
      tap((profile) => {
        this.currentProfile.set(profile);
        this.isLoading.set(false);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        if (err.status === 404) {
          this.currentProfile.set(null);
          return throwError(() => err);
        }
        const msg = err.error?.error || 'Erreur lors du chargement de votre profil nutritionnel.';
        this.errorMessage.set(msg);
        return throwError(() => new Error(msg));
      })
    );
  }

  /**
   * Sauvegarde les modifications du profil en base de données
   */
  saveProfile(profileData: Partial<NutritionProfile>): Observable<NutritionProfile> {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    return this.http.put<NutritionProfile>(`${this.baseUrl}/profile`, profileData).pipe(
      tap((updated) => {
        this.currentProfile.set(updated);
        this.isLoading.set(false);
        this.successMessage.set('Profil nutritionnel mis à jour avec succès et sauvegardé en base de données !');
      }),
      catchError((err) => {
        this.isLoading.set(false);
        const msg = err.error?.error || 'Erreur lors de la sauvegarde du profil.';
        this.errorMessage.set(msg);
        return throwError(() => new Error(msg));
      })
    );
  }

  /**
   * Réinitialise les alertes
   */
  clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }
}
