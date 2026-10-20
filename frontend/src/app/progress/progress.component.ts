import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FitnessApiService } from '../core/fitness-api.service';

interface Measurement {
  measuredOn: string;
  weightKg: number;
  waistCm?: number | null;
  bodyFatPercent?: number | null;
  notes?: string;
}

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './progress.component.html',
  styleUrl: './progress.component.scss'
})
export class ProgressComponent implements OnInit {
  private readonly storageKey = 'fitcoach_progress_v1';

  isAnalyzing = false;
  errorMessage = '';
  analysis: any = null;
  measurements: Measurement[] = [];

  readonly goalForm: FormGroup;
  readonly measureForm: FormGroup;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly fitnessApi: FitnessApiService
  ) {
    this.goalForm = this.formBuilder.nonNullable.group({
      firstName: ['', Validators.required],
      heightCm: [178, [Validators.required, Validators.min(100), Validators.max(250)]],
      mainGoal: ['fat loss', Validators.required],
      targetWeightKg: [75, [Validators.required, Validators.min(30), Validators.max(300)]],
      targetDate: ['']
    });

    this.measureForm = this.formBuilder.nonNullable.group({
      measuredOn: [this.today(), Validators.required],
      weightKg: [80, [Validators.required, Validators.min(30), Validators.max(300)]],
      waistCm: [null as number | null],
      bodyFatPercent: [null as number | null],
      notes: ['']
    });
  }

  ngOnInit(): void {
    this.loadFromStorage();
  }

  get sortedMeasurements(): Measurement[] {
    return [...this.measurements].sort((a, b) => b.measuredOn.localeCompare(a.measuredOn));
  }

  get spanDays(): number {
    if (this.measurements.length < 2) return 0;
    const dates = this.measurements.map(m => m.measuredOn).sort();
    const first = new Date(dates[0] + 'T00:00:00');
    const last = new Date(dates[dates.length - 1] + 'T00:00:00');
    return Math.round((last.getTime() - first.getTime()) / 86_400_000);
  }

  get chart(): { points: string; targetY: number; labels: { x: number; text: string }[]; weights: { x: number; y: number; value: number }[] } | null {
    const points = this.analysis?.points as Array<{ date: string; weightKg: number }> | undefined;
    const target = this.analysis?.stats?.targetWeightKg as number | undefined;
    if (!points?.length || target == null) return null;

    const width = 640;
    const height = 220;
    const pad = 28;
    const weights = points.map(p => p.weightKg);
    const minW = Math.min(...weights, target) - 1;
    const maxW = Math.max(...weights, target) + 1;
    const span = Math.max(maxW - minW, 1);

    const mapped = points.map((p, i) => {
      const x = pad + (points.length === 1 ? 0 : (i / (points.length - 1)) * (width - pad * 2));
      const y = height - pad - ((p.weightKg - minW) / span) * (height - pad * 2);
      return { x, y, value: p.weightKg, date: p.date };
    });

    const targetY = height - pad - ((target - minW) / span) * (height - pad * 2);
    const labels = mapped
      .filter((_, i) => i === 0 || i === mapped.length - 1 || mapped.length <= 5)
      .map(p => ({ x: p.x, text: p.date.slice(5) }));

    return {
      points: mapped.map(p => `${p.x},${p.y}`).join(' '),
      targetY,
      labels,
      weights: mapped
    };
  }

  get statusLabel(): string {
    switch (this.analysis?.status) {
      case 'PROGRESSING': return 'On track';
      case 'GOAL_REACHED': return 'Goal reached';
      case 'STABLE': return 'Stable';
      case 'MOVING_AWAY': return 'Needs adjustment';
      case 'INSUFFICIENT_DATA': return 'More data needed';
      default: return this.analysis?.source === 'ai' ? `AI · ${this.analysis.model}` : 'Report ready';
    }
  }

  get statusTitle(): string {
    switch (this.analysis?.status) {
      case 'PROGRESSING': return 'Good progress';
      case 'GOAL_REACHED': return 'Target weight reached';
      case 'STABLE': return 'Weight is stable';
      case 'MOVING_AWAY': return 'Trend is moving away from your goal';
      case 'INSUFFICIENT_DATA': return 'Not enough data yet';
      default: return 'Progress report';
    }
  }

  get statusClass(): string {
    const status = this.analysis?.status;
    if (status === 'PROGRESSING' || status === 'GOAL_REACHED') return 'ok';
    if (status === 'MOVING_AWAY') return 'bad';
    if (status === 'INSUFFICIENT_DATA' || status === 'STABLE') return 'warn';
    return '';
  }

  addMeasurement(): void {
    if (this.measureForm.invalid) {
      this.measureForm.markAllAsTouched();
      return;
    }

    const raw = this.measureForm.getRawValue();
    const measurement: Measurement = {
      measuredOn: raw.measuredOn,
      weightKg: Number(raw.weightKg),
      waistCm: raw.waistCm === null || raw.waistCm === '' ? null : Number(raw.waistCm),
      bodyFatPercent: raw.bodyFatPercent === null || raw.bodyFatPercent === '' ? null : Number(raw.bodyFatPercent),
      notes: raw.notes?.trim() || undefined
    };

    this.measurements = [
      ...this.measurements.filter(m => m.measuredOn !== measurement.measuredOn),
      measurement
    ];
    this.saveToStorage();
    this.analysis = null;
    this.errorMessage = '';
    this.measureForm.patchValue({ measuredOn: this.today(), notes: '' });
  }

  removeMeasurement(date: string): void {
    this.measurements = this.measurements.filter(m => m.measuredOn !== date);
    this.saveToStorage();
    this.analysis = null;
  }

  loadSample(): void {
    const today = new Date();
    const d = (daysAgo: number) => {
      const date = new Date(today);
      date.setDate(date.getDate() - daysAgo);
      return date.toISOString().slice(0, 10);
    };
    const target = new Date(today);
    target.setMonth(target.getMonth() + 4);

    this.goalForm.patchValue({
      firstName: 'Oussama',
      heightCm: 178,
      mainGoal: 'fat loss',
      targetWeightKg: 75,
      targetDate: target.toISOString().slice(0, 10)
    });
    this.measurements = [
      { measuredOn: d(28), weightKg: 80, waistCm: 90 },
      { measuredOn: d(21), weightKg: 79.5 },
      { measuredOn: d(14), weightKg: 79, waistCm: 89 },
      { measuredOn: d(7), weightKg: 78.5 },
      { measuredOn: d(0), weightKg: 78, waistCm: 88 }
    ];
    this.saveToStorage();
    this.analysis = null;
    this.errorMessage = '';
  }

  loadSampleAndAnalyze(): void {
    this.loadSample();
    this.analyze();
  }

  analyze(): void {
    if (this.goalForm.invalid) {
      this.goalForm.markAllAsTouched();
      this.errorMessage = 'Complete your goal details first (name is required).';
      return;
    }
    if (this.measurements.length === 0) {
      this.errorMessage = 'Add at least one measurement, or click “Try with sample data”.';
      return;
    }

    const goal = this.goalForm.getRawValue();
    this.isAnalyzing = true;
    this.errorMessage = '';
    this.analysis = null;

    this.fitnessApi.analyzeProgress({
      firstName: goal.firstName,
      heightCm: Number(goal.heightCm),
      mainGoal: goal.mainGoal,
      targetWeightKg: Number(goal.targetWeightKg),
      targetDate: goal.targetDate || null,
      measurements: this.measurements.map(m => ({
        measuredOn: m.measuredOn,
        weightKg: m.weightKg,
        waistCm: m.waistCm ?? null,
        bodyFatPercent: m.bodyFatPercent ?? null,
        notes: m.notes || null
      }))
    }).subscribe({
      next: result => {
        this.analysis = result;
        this.isAnalyzing = false;
        setTimeout(() => document.getElementById('progress-result')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
      },
      error: error => {
        this.isAnalyzing = false;
        this.errorMessage = error?.error?.error
          || Object.values(error?.error || {}).join(', ')
          || 'The progress coach could not answer. Check that the backend is running on port 8080.';
      }
    });
  }

  formatTrend(value: number | null | undefined): string {
    if (value == null) return '—';
    const sign = value > 0 ? '+' : '';
    return `${sign}${value} kg/week`;
  }

  formatSigned(value: number | null | undefined): string {
    if (value == null) return '—';
    const sign = value > 0 ? '+' : '';
    return `${sign}${value} kg`;
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private loadFromStorage(): void {
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) return;
      const data = JSON.parse(raw);
      if (data.goal) this.goalForm.patchValue(data.goal);
      if (Array.isArray(data.measurements)) this.measurements = data.measurements;
    } catch {
      localStorage.removeItem(this.storageKey);
    }
  }

  private saveToStorage(): void {
    localStorage.setItem(this.storageKey, JSON.stringify({
      goal: this.goalForm.getRawValue(),
      measurements: this.measurements
    }));
  }
}
