import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FitnessApiService } from '../core/fitness-api.service';
import { RecoverySummaryComponent } from '../recovery/recovery-summary.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RecoverySummaryComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  isGenerating = false;
  actionMessage = '';
  userName = 'Majd';
  goal = 'Build Muscle';
  currentWeight = 60;
  targetWeight = 65;
  bmi = 22.4;
  planProgress = 80;
  activitiesThisWeek = 3;
  todayWorkout = 'Chest + Triceps';
  aiMessage = 'You are progressing well. Keep your current training volume this week.';

  metrics = [
    { label: 'Workout completion', value: '4/5' },
    { label: 'Avg sleep', value: '7.1h' },
    { label: 'Strava this week', value: '3 activities' },
    { label: 'Calories target', value: '2450 kcal' }
  ];

  workouts = [
    { day: 'Mon', title: 'Upper body push', detail: 'Bench + incline press' },
    { day: 'Tue', title: 'Recovery walk', detail: '30 min zone 2' },
    { day: 'Wed', title: 'Lower body strength', detail: 'Squats and Romanian deadlift' },
    { day: 'Thu', title: 'Off day', detail: 'Mobility + sleep focus' },
    { day: 'Fri', title: 'Full body', detail: 'Pull-ups, row, press' }
  ];

  constructor(private readonly fitnessApi: FitnessApiService) {}

  generatePlan(): void {
    this.isGenerating = true;
    this.actionMessage = 'AI is building a plan from your profile...';
    this.fitnessApi.generatePlan().subscribe({
      next: () => {
        this.isGenerating = false;
        this.actionMessage = 'Your personalized workout plan is ready.';
      },
      error: () => {
        this.isGenerating = false;
        this.actionMessage = 'Complete your profile and sign in before generating a plan.';
      }
    });
  }

  connectStrava(): void {
    this.fitnessApi.connectStrava().subscribe({
      next: ({ url }) => window.location.assign(url),
      error: () => this.actionMessage = 'Add Strava credentials to the backend first.'
    });
  }

  syncStrava(): void {
    this.fitnessApi.syncStrava().subscribe({
      next: message => this.actionMessage = message,
      error: () => this.actionMessage = 'Sign in and connect Strava before syncing activities.'
    });
  }
}
