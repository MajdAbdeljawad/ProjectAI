import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ProgressEntryApiService } from '../../shared/progress-entry-api.service';
import { problemMessage, ProgressEntry, ProgressEntryRequest } from '../../shared/models';
import { UserContextService } from '../../shared/user-context.service';

@Component({
  selector: 'app-progress-page',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule],
  templateUrl: './progress-page.html',
  styleUrl: './progress-page.scss',
})
export class ProgressPage {
  private readonly api = inject(ProgressEntryApiService);
  private readonly userContext = inject(UserContextService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly reloadVersion = signal(0);

  readonly entries = signal<ProgressEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly loadError = signal('');
  readonly success = signal('');
  readonly formVisible = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly form = this.formBuilder.group({
    measurementDate: ['', Validators.required],
    weight: [null as number | null, [Validators.min(0.01), Validators.max(1000), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    waistCircumference: [null as number | null, [Validators.min(0.01), Validators.max(1000), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    sleepHours: [null as number | null, [Validators.min(0), Validators.max(24), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    energyLevel: [null as number | null, [Validators.min(1), Validators.max(5)]],
  });

  constructor() {
    let currentUserId = this.userContext.userId();
    effect(() => {
      const userId = this.userContext.userId();
      if (userId !== currentUserId) {
        currentUserId = userId;
        untracked(() => {
          this.cancelForm();
          this.success.set('');
          this.saving.set(false);
        });
      }
    });
    this.route.queryParamMap.subscribe((params) => {
      if (params.get('nouveau') === '1') this.beginCreate();
    });
    effect((onCleanup) => {
      const userId = this.userContext.userId();
      this.reloadVersion();
      this.loading.set(true);
      this.loadError.set('');
      this.entries.set([]);
      const subscription = this.api.list(userId).subscribe({
        next: (entries) => { this.entries.set(entries); this.loading.set(false); },
        error: (error: unknown) => { this.loadError.set(problemMessage(error)); this.loading.set(false); },
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  beginCreate(): void {
    if (this.saving()) return;
    this.editingId.set(null);
    const today = new Date();
    const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    this.form.reset({ measurementDate: localDate, weight: null,
      waistCircumference: null, sleepHours: null, energyLevel: null });
    this.formVisible.set(true);
    this.error.set('');
    this.success.set('');
  }

  beginEdit(entry: ProgressEntry): void {
    if (this.saving()) return;
    this.editingId.set(entry.id);
    this.form.reset({ measurementDate: entry.measurementDate, weight: entry.weight,
      waistCircumference: entry.waistCircumference, sleepHours: entry.sleepHours, energyLevel: entry.energyLevel });
    this.formVisible.set(true);
    this.error.set('');
    this.success.set('');
  }

  cancelForm(): void { this.formVisible.set(false); this.editingId.set(null); this.error.set(''); }
  refresh(): void { this.reloadVersion.update((value) => value + 1); }

  save(): void {
    if (this.saving()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    const request: ProgressEntryRequest = {
      measurementDate: value.measurementDate!,
      weight: this.optionalNumber(value.weight),
      waistCircumference: this.optionalNumber(value.waistCircumference),
      sleepHours: this.optionalNumber(value.sleepHours),
      energyLevel: this.optionalNumber(value.energyLevel),
    };
    this.saving.set(true);
    this.error.set('');
    const userId = this.userContext.userId();
    const id = this.editingId();
    const operation = id == null ? this.api.create(userId, request) : this.api.update(userId, id, request);
    operation.subscribe({
      next: () => {
        if (this.userContext.userId() !== userId) return;
        this.saving.set(false);
        this.formVisible.set(false);
        this.success.set(id == null ? 'Mesure ajoutée.' : 'Mesure modifiée.');
        this.refresh();
      },
      error: (error: unknown) => {
        if (this.userContext.userId() !== userId) return;
        this.saving.set(false); this.error.set(problemMessage(error));
      },
    });
  }

  delete(entry: ProgressEntry): void {
    if (!globalThis.confirm(`Supprimer la mesure du ${entry.measurementDate} ?`)) return;
    this.error.set('');
    this.success.set('');
    const userId = this.userContext.userId();
    this.api.delete(userId, entry.id).subscribe({
      next: () => {
        if (this.userContext.userId() !== userId) return;
        if (this.editingId() === entry.id) this.cancelForm();
        this.success.set('Mesure supprimée.'); this.refresh();
      },
      error: (error: unknown) => {
        if (this.userContext.userId() === userId) this.error.set(problemMessage(error));
      },
    });
  }

  private optionalNumber(value: number | string | null): number | null {
    if (value === '' || value == null) return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
