export interface RecoveryCheckin {
  id?: number;
  recordedOn?: string;
  recordedAt?: string;
  sleepHours?: number | null;
  sleepQuality?: number | null;
  fatigueLevel?: number | null;
  sorenessLevel?: number | null;
  sorenessAreas?: string | null;
  restingHeartRate?: number | null;
  symptoms?: string | null;
  limitations?: string | null;
  notes?: string | null;
  source?: string;
  normalizationNotes?: string[];
}

export interface RecoveryIndicators {
  profile?: {
    firstName?: string | null;
    mainGoal?: string | null;
    fitnessLevel?: string | null;
    trainingDaysPerWeek?: number | null;
    sessionDurationMinutes?: number | null;
    available: boolean;
    freshness?: string;
  };
  training?: {
    recentSessionCount?: number | null;
    totalMinutes7d?: number | null;
    averageHeartRate?: number | null;
    intensityLabel?: string | null;
    latestSessionAt?: string | null;
    available: boolean;
    freshness?: string;
  };
  sleep?: {
    latestHours?: number | null;
    averageHours7d?: number | null;
    trend?: string | null;
    latestDate?: string | null;
    available: boolean;
    freshness?: string;
  };
  subjective?: {
    fatigueLevel?: number | null;
    sorenessLevel?: number | null;
    sorenessAreas?: string | null;
    latestDate?: string | null;
    available: boolean;
    freshness?: string;
  };
  heartRate?: {
    latestRestingHeartRate?: number | null;
    personalBaseline?: number | null;
    deltaFromBaseline?: number | null;
    sampleCount?: number | null;
    latestDate?: string | null;
    available: boolean;
    baselineAvailable: boolean;
    freshness?: string;
  };
  upcomingSession?: {
    weekNumber?: number | null;
    dayNumber?: number | null;
    type?: string | null;
    notes?: string | null;
    plannedDate?: string | null;
    restDay?: boolean;
    available: boolean;
    freshness?: string;
  };
  symptoms?: {
    symptoms?: string | null;
    limitations?: string | null;
    latestDate?: string | null;
    available: boolean;
    freshness?: string;
  };
}

export interface RecoveryAssessment {
  id: number;
  generatedAt: string;
  status: string;
  classification?: string | null;
  recommendedAction?: string | null;
  confidence?: string | null;
  explanation?: string | null;
  nextSessionGuidance?: string | null;
  safetyGuidance?: string | null;
  safetyNotice?: string | null;
  recoverySuggestions?: string[];
  missingInformation?: string[];
  supportingEvidence?: string[];
  uncertainties?: string[];
  safetyFlags?: string[];
  indicators?: RecoveryIndicators;
  promptAVersion?: string;
  promptBVersion?: string;
  promptCVersion?: string;
  dataFreshnessHours?: number | null;
  requiresProfessionalEvaluation: boolean;
  planChangeAuthorized: boolean;
  providerError?: string | null;
}

export interface RecoveryOverview {
  latestAssessment?: RecoveryAssessment | null;
  indicators: RecoveryIndicators;
  missingFields: string[];
  dataQualityNotes: string[];
  contradictions: string[];
  safetyFlags: string[];
  recentCheckins: RecoveryCheckin[];
  minimumSignalPresent: boolean;
  safetyNotice: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: string;
}
