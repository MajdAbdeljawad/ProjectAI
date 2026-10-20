import { Routes } from '@angular/router';
import { OverviewPage } from './features/overview/overview-page';
import { GoalsPage } from './features/goals/goals-page';
import { ProgressPage } from './features/progress/progress-page';

export const routes: Routes = [
  { path: '', component: OverviewPage, title: 'Vue d’ensemble · Move' },
  { path: 'objectifs', component: GoalsPage, title: 'Objectifs · Move' },
  { path: 'progression', component: ProgressPage, title: 'Progression · Move' },
  { path: '**', redirectTo: '' },
];
