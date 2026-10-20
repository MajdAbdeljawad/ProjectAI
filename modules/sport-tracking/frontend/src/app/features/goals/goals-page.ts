import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { GoalApiService } from '../../shared/goal-api.service';
import { Goal, GoalRequest, GOAL_STATUSES, GOAL_TYPES, problemMessage } from '../../shared/models';
import { UserContextService } from '../../shared/user-context.service';

@Component({
  selector: 'app-goals-page',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule],
  templateUrl: './goals-page.html',
  styleUrl: './goals-page.scss',
})
export class GoalsPage {
  private readonly api = inject(GoalApiService);
  private readonly userContext = inject(UserContextService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly reloadVersion = signal(0);

  readonly goals = signal<Goal[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly loadError = signal('');
  readonly success = signal('');
  readonly formVisible = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly goalTypes = GOAL_TYPES;
  readonly goalStatuses = GOAL_STATUSES;
  readonly form = this.formBuilder.group({
    type: ['WEIGHT' as GoalRequest['type'], Validators.required],
    targetValue: [null as number | null, [Validators.required, Validators.min(0.01),
      Validators.max(99999999.99), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    unit: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(24)]],
    startDate: ['', Validators.required],
    targetDate: ['', Validators.required],
    status: ['ACTIVE' as GoalRequest['status'], Validators.required],
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
      this.goals.set([]);
      const subscription = this.api.list(userId).subscribe({
        next: (goals) => { this.goals.set(goals); this.loading.set(false); },
        error: (error: unknown) => { this.loadError.set(problemMessage(error)); this.loading.set(false); },
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  beginCreate(): void {
    if (this.saving()) return;
    this.editingId.set(null);
    this.form.reset({ type: 'WEIGHT', targetValue: null, unit: '', startDate: '', targetDate: '', status: 'ACTIVE' });
    this.formVisible.set(true);
    this.error.set('');
    this.success.set('');
  }

  beginEdit(goal: Goal): void {
    if (this.saving()) return;
    this.editingId.set(goal.id);
    this.form.reset({
      type: goal.type,
      targetValue: goal.targetValue,
      unit: goal.unit,
      startDate: goal.startDate,
      targetDate: goal.targetDate,
      status: goal.status,
    });
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
    const startDate = value.startDate ?? '';
    const targetDate = value.targetDate ?? '';
    if (targetDate < startDate) {
      this.error.set('La date cible doit être égale ou postérieure à la date de début.');
      return;
    }
    const request: GoalRequest = {
      type: value.type!,
      targetValue: Number(value.targetValue),
      unit: value.unit!.trim(),
      startDate: value.startDate!,
      targetDate: value.targetDate!,
      status: value.status!,
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
        this.success.set(id == null ? 'Objectif créé.' : 'Objectif modifié.');
        this.refresh();
      },
      error: (error: unknown) => {
        if (this.userContext.userId() !== userId) return;
        this.saving.set(false); this.error.set(problemMessage(error));
      },
    });
  }

  delete(goal: Goal): void {
    if (!globalThis.confirm(`Supprimer l’objectif « ${goal.targetValue} ${goal.unit} » ?`)) return;
    this.error.set('');
    this.success.set('');
    const userId = this.userContext.userId();
    this.api.delete(userId, goal.id).subscribe({
      next: () => {
        if (this.userContext.userId() !== userId) return;
        if (this.editingId() === goal.id) this.cancelForm();
        this.success.set('Objectif supprimé.'); this.refresh();
      },
      error: (error: unknown) => {
        if (this.userContext.userId() === userId) this.error.set(problemMessage(error));
      },
    });
  }

  typeLabel(type: string): string { return this.goalTypes.find((item) => item.value === type)?.label ?? type; }
  statusLabel(status: string): string { return this.goalStatuses.find((item) => item.value === status)?.label ?? status; }
}
