import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MealPlanService, HistoryItem } from '../../../core/services/meal-plan.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="max-w-5xl mx-auto px-4 py-8">
      
      <!-- En-tête -->
      <div class="flex flex-col md:flex-row md:items-center justify-between pb-6 mb-8 border-b border-slate-200">
        <div>
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-600 font-bold text-xl">
              📜
            </div>
            <div>
              <h1 class="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight">
                Historique des Programmes Nutritionnels
              </h1>
              <p class="text-sm text-slate-500 mt-0.5">
                Retrouvez et réactivez vos anciens programmes générés par l'IA.
              </p>
            </div>
          </div>
        </div>

        <div class="mt-4 md:mt-0">
          <a 
            routerLink="/meal-plan" 
            [queryParams]="{ generate: 'true' }"
            class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm shadow-sm transition transform active:scale-95">
            <span>✨</span> Nouveau programme IA
          </a>
        </div>
      </div>

      <!-- Liste des programmes -->
      @if (history().length === 0) {
        <div class="text-center py-16 px-4 bg-white rounded-3xl border border-slate-200/80 shadow-sm">
          <div class="text-4xl mb-3">📁</div>
          <h3 class="text-lg font-bold text-slate-900 mb-1">Aucun historique disponible</h3>
          <p class="text-xs text-slate-500 mb-5">Vous n'avez pas encore généré de programmes alimentaires.</p>
          <a 
            routerLink="/meal-plan" 
            [queryParams]="{ generate: 'true' }"
            class="inline-block px-5 py-2.5 rounded-xl bg-emerald-600 text-white font-bold text-xs">
            Générer mon premier programme
          </a>
        </div>
      } @else {
        <div class="space-y-4">
          @for (item of history(); track item.id) {
            <div 
              class="bg-white rounded-2xl border transition-all p-5 shadow-sm hover:shadow-md flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              [class.border-emerald-500]="item.isActive"
              [class.border-slate-200/80]="!item.isActive">
              
              <div class="flex items-center gap-4">
                <div 
                  class="w-12 h-12 rounded-2xl flex items-center justify-center text-xl font-bold"
                  [class.bg-emerald-100]="item.isActive"
                  [class.text-emerald-700]="item.isActive"
                  [class.bg-slate-100]="!item.isActive"
                  [class.text-slate-600]="!item.isActive">
                  {{ item.isActive ? '⭐' : '📋' }}
                </div>

                <div>
                  <div class="flex items-center gap-2">
                    <h3 class="font-bold text-base text-slate-900">{{ item.title }}</h3>
                    @if (item.isActive) {
                      <span class="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-extrabold uppercase tracking-wider">
                        Actif
                      </span>
                    }
                  </div>
                  <div class="text-xs text-slate-500 mt-1 flex flex-wrap gap-3">
                    <span>📅 Créé le {{ formatDate(item.createdAt) }}</span>
                    <span>• ⏱️ Durée : {{ item.durationDays }} jours</span>
                    <span>• 🔥 Cible : {{ item.targetCalories }} kcal/j</span>
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="flex items-center gap-2 self-end sm:self-center">
                @if (!item.isActive) {
                  <button 
                    type="button"
                    (click)="activate(item.id)"
                    class="px-3.5 py-2 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-800 text-xs font-bold transition cursor-pointer">
                    Activer ce programme
                  </button>
                }
                <button 
                  type="button"
                  (click)="viewPlan(item.id)"
                  class="px-3.5 py-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold cursor-pointer">
                  Consulter
                </button>
                <button 
                  type="button"
                  (click)="deletePlan(item.id)"
                  class="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition cursor-pointer"
                  title="Supprimer">
                  🗑️
                </button>
              </div>

            </div>
          }
        </div>
      }

    </div>
  `
})
export class HistoryComponent implements OnInit {
  private router = inject(Router);
  public mealPlanService = inject(MealPlanService);

  public history = this.mealPlanService.history;

  ngOnInit(): void {
    this.mealPlanService.loadHistory().subscribe();
  }

  formatDate(isoString: string): string {
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  }

  activate(id: string): void {
    this.mealPlanService.activatePlan(id).subscribe({
      next: () => {
        this.router.navigate(['/meal-plan']);
      }
    });
  }

  viewPlan(id: string): void {
    this.mealPlanService.activatePlan(id).subscribe({
      next: () => {
        this.router.navigate(['/meal-plan']);
      }
    });
  }

  deletePlan(id: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce programme de votre historique ?')) {
      this.mealPlanService.deletePlan(id).subscribe();
    }
  }
}
