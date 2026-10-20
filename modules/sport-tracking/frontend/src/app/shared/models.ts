export type GoalType = 'WEIGHT' | 'WAIST' | 'SLEEP' | 'ENERGY' | 'WORKOUTS' | 'DISTANCE' | 'CUSTOM';
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED' | 'CANCELLED';

export interface Goal {
  id: number;
  userId: number;
  type: GoalType;
  targetValue: number;
  unit: string;
  startDate: string;
  targetDate: string;
  status: GoalStatus;
}

export interface GoalRequest {
  type: GoalType;
  targetValue: number;
  unit: string;
  startDate: string;
  targetDate: string;
  status: GoalStatus;
}

export interface ProgressEntry {
  id: number;
  userId: number;
  measurementDate: string;
  weight: number | null;
  waistCircumference: number | null;
  sleepHours: number | null;
  energyLevel: number | null;
}

export interface ProgressEntryRequest {
  measurementDate: string;
  weight: number | null;
  waistCircumference: number | null;
  sleepHours: number | null;
  energyLevel: number | null;
}

export interface ApiProblem {
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}

export const GOAL_TYPES: ReadonlyArray<{ value: GoalType; label: string }> = [
  { value: 'WEIGHT', label: 'Poids' },
  { value: 'WAIST', label: 'Tour de taille' },
  { value: 'SLEEP', label: 'Sommeil' },
  { value: 'ENERGY', label: 'Énergie' },
  { value: 'WORKOUTS', label: 'Entraînements' },
  { value: 'DISTANCE', label: 'Distance' },
  { value: 'CUSTOM', label: 'Personnalisé' },
];

export const GOAL_STATUSES: ReadonlyArray<{ value: GoalStatus; label: string }> = [
  { value: 'ACTIVE', label: 'En cours' },
  { value: 'COMPLETED', label: 'Terminé' },
  { value: 'PAUSED', label: 'En pause' },
  { value: 'CANCELLED', label: 'Annulé' },
];

export function problemMessage(error: unknown): string {
  const body = (error as { error?: ApiProblem } | null)?.error;
  if (body?.errors && Object.keys(body.errors).length) {
    return Object.values(body.errors).join(' ');
  }
  if (body?.detail) return body.detail;
  if ((error as { status?: number } | null)?.status === 0) {
    return 'Le serveur est injoignable. Vérifiez que Spring Boot fonctionne sur le port 8081.';
  }
  return 'Une erreur est survenue. Réessayez dans un instant.';
}
