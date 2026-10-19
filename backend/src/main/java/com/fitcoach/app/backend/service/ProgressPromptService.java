package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.ProgressAnalysisRequest;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressPoint;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressStats;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgressPromptService {

    public static final String SYSTEM_PROMPT =
            "You are a careful, encouraging fitness coach. You answer with strict JSON only, never markdown.";

    public String buildProgressPrompt(ProgressAnalysisRequest request, ProgressAnalysisResponse analysis) {
        ProgressStats stats = analysis.getStats();
        return """
            Comment on the user's weight progress. The numbers below are already calculated by the app: never invent or change them.

            USER PROFILE
            - Name: %s
            - Height: %s cm
            - Main goal: %s
            - Target weight: %s kg
            - Target date: %s

            CALCULATED STATUS
            - Status: %s
            - Coach summary from the app: %s
            - Start weight: %s kg
            - Current weight: %s kg
            - Change so far: %s kg
            - Remaining to goal: %s kg
            - Progress to goal: %s%%
            - Weekly trend: %s kg/week
            - Estimated goal date: %s
            - Required weekly pace for target date: %s kg/week
            - On track for target date: %s
            - Current BMI: %s
            - Waist change: %s cm
            - Body-fat change: %s%%
            - Days tracked: %s
            - Safety warnings: %s

            RECENT MEASUREMENTS (oldest to newest)
            %s

            RULES
            - Return strict JSON only.
            - Do not invent numbers, dates, or medical diagnoses.
            - Use only the calculated status and measurements above.
            - Be encouraging and practical. Prefer habits over extreme diets.
            - Never prescribe through pain. Recommend a professional for medical concerns.
            - coachComment must be 2 to 4 sentences.
            - tips must contain exactly 3 short actionable tips.

            JSON FORMAT
            {
              "coachComment": "short paragraph",
              "tips": ["tip 1", "tip 2", "tip 3"]
            }
            """.formatted(
                request.getFirstName(),
                request.getHeightCm(),
                request.getMainGoal(),
                request.getTargetWeightKg(),
                request.getTargetDate() == null ? "none" : request.getTargetDate(),
                analysis.getStatus(),
                analysis.getMessage(),
                stats.getStartWeightKg(),
                stats.getCurrentWeightKg(),
                stats.getChangeKg(),
                stats.getRemainingKg(),
                stats.getPercentToGoal(),
                valueOr(stats.getWeeklyTrendKg(), "not enough data"),
                valueOr(stats.getEstimatedGoalDate(), "not available"),
                valueOr(stats.getRequiredWeeklyKg(), "not set"),
                valueOr(stats.getOnTrack(), "unknown"),
                stats.getCurrentBmi(),
                valueOr(stats.getWaistChangeCm(), "not tracked"),
                valueOr(stats.getBodyFatChangePercent(), "not tracked"),
                stats.getDaysTracked(),
                analysis.getWarnings().isEmpty() ? "none" : String.join("; ", analysis.getWarnings()),
                formatMeasurements(analysis.getPoints())
        );
    }

    private String formatMeasurements(List<ProgressPoint> points) {
        List<ProgressPoint> recent = points.size() <= 8 ? points : points.subList(points.size() - 8, points.size());
        return recent.stream()
                .map(p -> "- %s: weight %s kg, waist %s, body fat %s, BMI %s%s".formatted(
                        p.getDate(),
                        p.getWeightKg(),
                        valueOr(p.getWaistCm(), "-"),
                        valueOr(p.getBodyFatPercent(), "-"),
                        p.getBmi(),
                        p.getNotes() == null || p.getNotes().isBlank() ? "" : ", notes: " + p.getNotes()
                ))
                .collect(Collectors.joining("\n"));
    }

    private Object valueOr(Object value, String fallback) {
        return value == null ? fallback : value;
    }
}
