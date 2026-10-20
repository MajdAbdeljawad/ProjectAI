import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { GoalApiService } from '../../shared/goal-api.service';
import { Goal, GOAL_STATUSES, GOAL_TYPES, problemMessage, ProgressEntry } from '../../shared/models';
import { ProgressEntryApiService } from '../../shared/progress-entry-api.service';
import { UserContextService } from '../../shared/user-context.service';

@Component({
  selector: 'app-overview-page',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './overview-page.html',
  styleUrl: './overview-page.scss',
})
export class OverviewPage {
  private readonly goalsApi = inject(GoalApiService);
  private readonly progressApi = inject(ProgressEntryApiService);
  private readonly userContext = inject(UserContextService);
  private readonly reloadVersion = signal(0);

  readonly goals = signal<Goal[]>([]);
  readonly entries = signal<ProgressEntry[]>([]);
  readonly goalsError = signal('');
  readonly entriesError = signal('');
  readonly goalsLoading = signal(true);
  readonly entriesLoading = signal(true);
  readonly goalTypes = GOAL_TYPES;
  readonly goalStatuses = GOAL_STATUSES;

  constructor() {
    effect((onCleanup) => {
      const userId = this.userContext.userId();
      this.reloadVersion();
      this.goalsLoading.set(true);
      this.entriesLoading.set(true);
      this.goalsError.set('');
      this.entriesError.set('');
      const goalsSubscription = this.goalsApi.list(userId).subscribe({
        next: (goals) => { this.goals.set(goals); this.goalsLoading.set(false); },
        error: (error: unknown) => { this.goalsError.set(problemMessage(error)); this.goalsLoading.set(false); },
      });
      const entriesSubscription = this.progressApi.list(userId).subscribe({
        next: (entries) => { this.entries.set(entries.slice(0, 5)); this.entriesLoading.set(false); },
        error: (error: unknown) => { this.entriesError.set(problemMessage(error)); this.entriesLoading.set(false); },
      });
      onCleanup(() => { goalsSubscription.unsubscribe(); entriesSubscription.unsubscribe(); });
    });
  }

  refresh(): void { this.reloadVersion.update((value) => value + 1); }

  typeLabel(type: string): string {
    return this.goalTypes.find((item) => item.value === type)?.label ?? type;
  }

  statusLabel(status: string): string {
    return this.goalStatuses.find((item) => item.value === status)?.label ?? status;
  }

  measurementSummary(entry: ProgressEntry): string {
    const values = [
      entry.weight == null ? '' : `${entry.weight} kg`,
      entry.waistCircumference == null ? '' : `${entry.waistCircumference} cm`,
      entry.sleepHours == null ? '' : `${entry.sleepHours} h de sommeil`,
      entry.energyLevel == null ? '' : `Énergie ${entry.energyLevel}/5`,
    ].filter(Boolean);
    return values.length ? values.join(' · ') : 'Aucune valeur renseignée';
  }
}
