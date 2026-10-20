import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FitnessApiService } from '../core/fitness-api.service';
import { RecoveryOverview } from '../core/recovery.models';

@Component({
  selector: 'app-recovery-summary',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="panel">
      <div class="panel-header">
        <h3>Recovery</h3>
        <a routerLink="/recovery">Open recovery</a>
      </div>
      <p *ngIf="loading">Loading available recovery indicators...</p>
      <p *ngIf="!loading && authRequired">Sign in to generate a recovery assessment from your own data.</p>
      <ng-container *ngIf="!loading && overview">
        <p class="classification">{{ overview.latestAssessment ? overview.latestAssessment.classification : 'No assessment yet' }}</p>
        <p>{{ overview.latestAssessment?.nextSessionGuidance || 'Generate an assessment after logging sleep, fatigue, or training data. Missing values stay blank.' }}</p>
        <small>{{ overview.missingFields.length }} missing indicator{{ overview.missingFields.length === 1 ? '' : 's' }}</small>
      </ng-container>
    </section>
  `,
  styles: [`
    .panel { background: rgba(15, 23, 42, 0.78); border: 1px solid rgba(148, 163, 184, 0.16); border-radius: 1.1rem; padding: 1.1rem; }
    .panel-header { display: flex; justify-content: space-between; align-items: center; }
    h3, p, small { margin: 0.35rem 0; }
    a { color: #34d399; text-decoration: none; }
    .classification { color: #edf6ff; font-weight: 700; }
    p, small { color: #91a4bd; }
  `]
})
export class RecoverySummaryComponent implements OnInit {
  overview: RecoveryOverview | null = null;
  loading = true;
  authRequired = false;

  constructor(private readonly fitnessApi: FitnessApiService) {}

  ngOnInit(): void {
    this.fitnessApi.getRecoveryOverview().subscribe({
      next: overview => {
        this.overview = overview;
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.authRequired = error?.status === 401 || error?.status === 403;
      }
    });
  }
}
