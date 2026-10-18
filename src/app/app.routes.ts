import { Routes } from '@angular/router';
import { DashboardComponent } from './features/nutrition/dashboard/dashboard.component';
import { ProfileComponent } from './features/nutrition/profile/profile.component';
import { MealPlanComponent } from './features/nutrition/meal-plan/meal-plan.component';
import { HistoryComponent } from './features/nutrition/history/history.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent, title: 'Tableau de bord - Nutrition IA' },
  { path: 'profile', component: ProfileComponent, title: 'Mon Profil Nutritionnel - Nutrition IA' },
  { path: 'meal-plan', component: MealPlanComponent, title: 'Mon Programme 7 Jours - Nutrition IA' },
  { path: 'history', component: HistoryComponent, title: 'Historique des Programmes - Nutrition IA' },
  { path: '**', redirectTo: 'dashboard' }
];
