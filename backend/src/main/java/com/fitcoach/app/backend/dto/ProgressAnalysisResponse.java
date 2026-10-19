package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressAnalysisResponse {
    private String title;
    private ProgressStatus status;
    private String message;
    private ProgressStats stats;
    @Builder.Default
    private List<ProgressPoint> points = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    private String coachComment;
    @Builder.Default
    private List<String> tips = new ArrayList<>();
    private String source;
    private String model;
    private String prompt;

    public enum ProgressStatus {
        INSUFFICIENT_DATA,
        GOAL_REACHED,
        PROGRESSING,
        STABLE,
        MOVING_AWAY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressStats {
        private Double startWeightKg;
        private Double currentWeightKg;
        private Double targetWeightKg;
        private Double changeKg;
        private Double remainingKg;
        private Integer percentToGoal;
        private Double weeklyTrendKg;
        private LocalDate estimatedGoalDate;
        private Double requiredWeeklyKg;
        private Boolean onTrack;
        private Double currentBmi;
        private Double waistChangeCm;
        private Double bodyFatChangePercent;
        private Integer daysTracked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressPoint {
        private LocalDate date;
        private Double weightKg;
        private Double waistCm;
        private Double bodyFatPercent;
        private Double bmi;
        private String notes;
    }
}
