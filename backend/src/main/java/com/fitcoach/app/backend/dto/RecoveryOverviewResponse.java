package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryOverviewResponse {
    private RecoveryAssessmentResponse latestAssessment;
    private Indicators indicators;
    @Builder.Default
    private List<String> missingFields = new ArrayList<>();
    @Builder.Default
    private List<String> dataQualityNotes = new ArrayList<>();
    @Builder.Default
    private List<String> contradictions = new ArrayList<>();
    @Builder.Default
    private List<String> safetyFlags = new ArrayList<>();
    @Builder.Default
    private List<RecoveryCheckinResponse> recentCheckins = new ArrayList<>();
    private boolean minimumSignalPresent;
    private String safetyNotice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Indicators {
        private ProfileIndicator profile;
        private TrainingIndicator training;
        private SleepIndicator sleep;
        private SubjectiveIndicator subjective;
        private HeartRateIndicator heartRate;
        private UpcomingSessionIndicator upcomingSession;
        private SymptomIndicator symptoms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileIndicator {
        private String firstName;
        private String mainGoal;
        private String fitnessLevel;
        private Integer trainingDaysPerWeek;
        private Integer sessionDurationMinutes;
        private boolean available;
        private String freshness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingIndicator {
        private Integer recentSessionCount;
        private Long totalMinutes7d;
        private Double averageHeartRate;
        private String intensityLabel;
        private LocalDateTime latestSessionAt;
        private boolean available;
        private String freshness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepIndicator {
        private Double latestHours;
        private Double averageHours7d;
        private String trend;
        private LocalDate latestDate;
        private boolean available;
        private String freshness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectiveIndicator {
        private Integer fatigueLevel;
        private Integer sorenessLevel;
        private String sorenessAreas;
        private LocalDate latestDate;
        private boolean available;
        private String freshness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeartRateIndicator {
        private Integer latestRestingHeartRate;
        private Double personalBaseline;
        private Double deltaFromBaseline;
        private Integer sampleCount;
        private LocalDate latestDate;
        private boolean available;
        private boolean baselineAvailable;
        private String freshness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingSessionIndicator {
        private Integer weekNumber;
        private Integer dayNumber;
        private String type;
        private String notes;
        private LocalDate plannedDate;
        private boolean restDay;
        private boolean available;
        private String freshness;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymptomIndicator {
        private String symptoms;
        private String limitations;
        private LocalDate latestDate;
        private boolean available;
        private String freshness;
    }
}
