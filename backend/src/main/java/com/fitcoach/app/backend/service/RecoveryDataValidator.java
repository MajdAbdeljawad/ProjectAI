package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecoveryCheckinRequest;
import com.fitcoach.app.backend.exception.ApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecoveryDataValidator {

    public static final double SLEEP_HOURS_MIN = 0.0;
    public static final double SLEEP_HOURS_MAX = 16.0;
    public static final double SLEEP_MINUTES_MIN = 30.0;
    public static final double SLEEP_MINUTES_MAX = 960.0;
    public static final int RHR_TYPICAL_MIN = 30;
    public static final int RHR_TYPICAL_MAX = 120;

    private RecoveryDataValidator() {
    }

    public static NormalizedSleep normalizeSleep(Double rawSleepHours) {
        if (rawSleepHours == null) {
            return new NormalizedSleep(null, List.of());
        }
        if (rawSleepHours < SLEEP_HOURS_MIN) {
            throw new ApiException("Sleep duration cannot be negative", org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        if (rawSleepHours >= SLEEP_MINUTES_MIN && rawSleepHours <= SLEEP_MINUTES_MAX && rawSleepHours > SLEEP_HOURS_MAX) {
            double hours = Math.round((rawSleepHours / 60.0) * 10.0) / 10.0;
            return new NormalizedSleep(hours, List.of(
                    "Interpreted sleep input " + rawSleepHours + " as minutes (" + hours + " hours). Original value was preserved as a unit conversion, not invented."
            ));
        }
        if (rawSleepHours > SLEEP_HOURS_MAX) {
            throw new ApiException("Sleep duration must be 0-16 hours, or 30-960 if entered as minutes",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        return new NormalizedSleep(rawSleepHours, List.of());
    }

    public static void rejectFutureDate(LocalDate recordedOn, LocalDate today) {
        if (recordedOn != null && recordedOn.isAfter(today)) {
            throw new ApiException("Check-in date cannot be in the future",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
    }

    public static List<String> inspectHeartRate(Integer restingHeartRate) {
        List<String> notes = new ArrayList<>();
        if (restingHeartRate == null) {
            return notes;
        }
        if (restingHeartRate < RHR_TYPICAL_MIN || restingHeartRate > RHR_TYPICAL_MAX) {
            notes.add("Resting heart rate " + restingHeartRate + " bpm is outside the typical resting range ("
                    + RHR_TYPICAL_MIN + "-" + RHR_TYPICAL_MAX + "). It was kept as reported and was not replaced.");
        }
        return notes;
    }

    public static List<String> detectSubjectiveContradictions(Double sleepHours,
                                                             Integer fatigueLevel,
                                                             Integer sorenessLevel,
                                                             String notes,
                                                             String symptoms) {
        List<String> contradictions = new ArrayList<>();
        String combined = ((notes == null ? "" : notes) + " " + (symptoms == null ? "" : symptoms))
                .toLowerCase(Locale.ROOT);

        if (sleepHours != null && sleepHours < 5 && containsAny(combined, "slept well", "great sleep", "well rested")) {
            contradictions.add("Reported sleep is under 5 hours but notes describe sleeping well.");
        }
        if (sleepHours != null && sleepHours >= 8 && fatigueLevel != null && fatigueLevel >= 8) {
            contradictions.add("Sleep duration is 8+ hours while fatigue is reported as high.");
        }
        if (fatigueLevel != null && fatigueLevel <= 2 && sleepHours != null && sleepHours < 4) {
            contradictions.add("Fatigue is very low while sleep duration is under 4 hours.");
        }
        if (fatigueLevel != null && fatigueLevel >= 8 && containsAny(combined, "fresh", "great energy", "feeling strong")) {
            contradictions.add("Fatigue is high but notes describe feeling fresh or energetic.");
        }
        if (sorenessLevel != null && sorenessLevel >= 8 && containsAny(combined, "no soreness", "not sore")) {
            contradictions.add("Soreness rating is high but notes say there is no soreness.");
        }
        return contradictions;
    }

    public static String freshnessLabel(Integer ageHours, int freshUntilHours, int staleAfterHours) {
        if (ageHours == null) {
            return "missing";
        }
        if (ageHours <= freshUntilHours) {
            return "fresh";
        }
        if (ageHours <= staleAfterHours) {
            return "aging";
        }
        return "stale";
    }

    public static Double median(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Integer> sorted = values.stream().sorted().toList();
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2).doubleValue();
        }
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }

    public static String trendFromAverages(Double recentAverage, Double previousAverage) {
        if (recentAverage == null || previousAverage == null || previousAverage == 0) {
            return "unknown";
        }
        double delta = recentAverage - previousAverage;
        if (Math.abs(delta) < 0.3) {
            return "stable";
        }
        return delta > 0 ? "improving" : "declining";
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record NormalizedSleep(Double hours, List<String> notes) {
    }
}
