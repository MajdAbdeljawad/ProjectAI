import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { IntakeComponent } from './intake/intake.component';
import { RecoveryComponent } from './recovery/recovery.component';
import { SectionPlaceholderComponent } from './shared/section-placeholder.component';

export const routes: Routes = [
  { path: '', component: IntakeComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'recovery', component: RecoveryComponent },
  { path: 'diet', component: SectionPlaceholderComponent, data: { title: 'Diet' } },
  { path: 'progress', component: ProgressComponent },
  { path: 'strava', component: SectionPlaceholderComponent, data: { title: 'Strava' } },
  { path: 'profile', component: SectionPlaceholderComponent, data: { title: 'Profile' } },
  { path: '**', redirectTo: '' }
];
