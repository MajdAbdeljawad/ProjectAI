import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FitnessApiService } from '../core/fitness-api.service';
import { RecoveryAssessment, RecoveryCheckin, RecoveryOverview } from '../core/recovery.models';

type RecoveryViewState = 'loading' | 'empty' | 'success' | 'validation-error' | 'provider-error' | 'auth-required';

@Component({
  selector: 'app-recovery',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './recovery.component.html',
  styleUrl: './recovery.component.scss'
})
export class RecoveryComponent implements OnInit {
  viewState: RecoveryViewState = 'loading';
  generating = false;
  authenticating = false;
  errorMessage = '';
  successMessage = '';
  overview: RecoveryOverview | null = null;
  assessment: RecoveryAssessment | null = null;
  history: RecoveryAssessment[] = [];
  readonly checkinForm: FormGroup;
  readonly authForm: FormGroup;

  constructor(
    private readonly fitnessApi: FitnessApiService,
    formBuilder: FormBuilder
  ) {
    this.checkinForm = formBuilder.nonNullable.group({
      sleepHours: [''],
      sleepQuality: [''],
      fatigueLevel: [''],
      sorenessLevel: [''],
      sorenessAreas: [''],
      restingHeartRate: [''],
      symptoms: [''],
      limitations: [''],
      notes: ['']
    });
    this.authForm = formBuilder.nonNullable.group({
      email: [''],
      password: [''],
      firstName: [''],
      lastName: ['']
    });
  }

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.viewState = 'loading';
    this.errorMessage = '';
    this.fitnessApi.getRecoveryOverview().subscribe({
      next: overview => {
        this.overview = overview;
        this.assessment = overview.latestAssessment ?? null;
        this.viewState = this.assessment ? this.stateFromAssessment(this.assessment) : 'empty';
        this.loadHistory();
      },
      error: error => this.handleError(error, true)
    });
  }

  generate(): void {
    this.generating = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.viewState = 'loading';
    this.fitnessApi.generateRecoveryAssessment(this.buildCheckinPayload()).subscribe({
      next: assessment => {
        this.generating = false;
        this.assessment = assessment;
        this.viewState = this.stateFromAssessment(assessment);
        this.successMessage = assessment.status === 'SUCCESS'
          ? 'Recovery assessment updated from your current data.'
          : '';
        this.refreshQuietly();
      },
      error: error => {
        this.generating = false;
        this.handleError(error, false);
      }
    });
  }

  login(): void {
    const { email, password } = this.authForm.getRawValue();
    this.authenticating = true;
    this.fitnessApi.login(email, password).subscribe({
      next: response => {
        this.fitnessApi.storeToken(response.token);
        this.authenticating = false;
        this.refresh();
      },
      error: () => {
        this.authenticating = false;
        this.errorMessage = 'Sign-in failed. Check your email and password.';
      }
    });
  }

  register(): void {
    const { email, password, firstName, lastName } = this.authForm.getRawValue();
    this.authenticating = true;
    this.fitnessApi.register({ email, password, firstName, lastName }).subscribe({
      next: response => {
        this.fitnessApi.storeToken(response.token);
        this.authenticating = false;
        this.refresh();
      },
      error: () => {
        this.authenticating = false;
        this.errorMessage = 'Registration failed. Use a unique email and a password of at least 8 characters.';
      }
    });
  }

  displayValue(value: unknown, fallback = 'Not available'): string {
    if (value === null || value === undefined || value === '') {
      return fallback;
    }
    return String(value);
  }

  actionLabel(action?: string | null): string {
    switch (action) {
      case 'CONTINUE_PLAN': return 'Continue the plan';
      case 'REDUCE_INTENSITY': return 'Reduce intensity';
      case 'REDUCE_VOLUME': return 'Reduce volume';
      case 'LIGHT_RECOVERY': return 'Choose light recovery activity';
      case 'REST': return 'Rest';
      default: return 'Waiting for an assessment';
    }
  }

  classificationLabel(classification?: string | null): string {
    switch (classification) {
      case 'FAVORABLE': return 'Favorable';
      case 'MIXED': return 'Mixed';
      case 'ADDITIONAL_RECOVERY_RECOMMENDED': return 'Additional recovery recommended';
      case 'INSUFFICIENT_INFORMATION': return 'Insufficient information';
      default: return 'No classification yet';
    }
  }

  private loadHistory(): void {
    this.fitnessApi.getRecoveryHistory().subscribe({
      next: history => this.history = history,
      error: () => this.history = []
    });
  }

  private refreshQuietly(): void {
    this.fitnessApi.getRecoveryOverview().subscribe({
      next: overview => {
        this.overview = overview;
        this.loadHistory();
      },
      error: () => undefined
    });
  }

  private buildCheckinPayload(): Partial<RecoveryCheckin> | null {
    const raw = this.checkinForm.getRawValue() as Record<string, unknown>;
    const payload: Partial<RecoveryCheckin> = {};
    const numberFields: Array<keyof RecoveryCheckin> = [
      'sleepHours', 'sleepQuality', 'fatigueLevel', 'sorenessLevel', 'restingHeartRate'
    ];
    numberFields.forEach(field => {
      const value = raw[String(field)];
      if (value !== '' && value !== null && value !== undefined) {
        (payload as Record<string, number>)[String(field)] = Number(value);
      }
    });
    ['sorenessAreas', 'symptoms', 'limitations', 'notes'].forEach(field => {
      const value = String(raw[field] ?? '').trim();
      if (value) {
        (payload as Record<string, string>)[field] = value;
      }
    });
    return Object.keys(payload).length ? payload : null;
  }

  private stateFromAssessment(assessment: RecoveryAssessment): RecoveryViewState {
    if (assessment.status === 'PROVIDER_ERROR') {
      return 'provider-error';
    }
    if (assessment.status === 'INSUFFICIENT_DATA' || assessment.status === 'SAFETY_HOLD' || assessment.status === 'SUCCESS') {
      return 'success';
    }
    return 'success';
  }

  private handleError(error: unknown, fromLoad: boolean): void {
    const httpError = error as HttpErrorResponse;
    if (httpError?.status === 401 || httpError?.status === 403) {
      this.viewState = 'auth-required';
      this.errorMessage = 'Sign in to view and generate recovery assessments.';
      return;
    }
    if (httpError?.status === 400) {
      this.viewState = 'validation-error';
      this.errorMessage = httpError.error?.error || 'The check-in could not be validated. Missing values were not replaced.';
      return;
    }
    if (httpError?.status === 0 || httpError?.status >= 500) {
      this.viewState = 'provider-error';
      this.errorMessage = httpError.error?.error || 'The recovery service is unavailable. No measurements were invented.';
      return;
    }
    this.viewState = fromLoad ? 'empty' : 'provider-error';
    this.errorMessage = 'Unable to load recovery data.';
  }
}
