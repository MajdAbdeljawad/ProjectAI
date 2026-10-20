package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.ProgressAnalysisRequest;
import com.fitcoach.app.backend.dto.ProgressAnalysisRequest.MeasurementInput;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressPoint;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressStats;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
public class ProgressCalculationsService {

    static final double GOAL_TOLERANCE_KG = 0.5;
    static final double STABLE_THRESHOLD_KG_PER_WEEK = 0.1;
    static final int MIN_DAYS_FOR_TREND = 7;
    static final double MAX_SAFE_WEEKLY_RATE = 0.01;
    static final int LONG_ESTIMATE_WEEKS = 104;

    public ProgressAnalysisResponse analyze(ProgressAnalysisRequest request) {
        List<MeasurementInput> sorted = request.getMeasurements().stream()
                .sorted(Comparator.comparing(MeasurementInput::getMeasuredOn))
                .toList();
        MeasurementInput first = sorted.get(0);
        MeasurementInput last = sorted.get(sorted.size() - 1);

        double start = first.getWeightKg();
        double current = last.getWeightKg();
        double target = request.getTargetWeightKg();
        int daysTracked = (int) ChronoUnit.DAYS.between(first.getMeasuredOn(), last.getMeasuredOn());
        List<String> warnings = new ArrayList<>();

        ProgressStats stats = ProgressStats.builder()
                .startWeightKg(start)
                .currentWeightKg(current)
                .targetWeightKg(target)
                .changeKg(round(current - start, 1))
                .remainingKg(round(Math.abs(target - current), 1))
                .percentToGoal(percentToGoal(start, current, target))
                .currentBmi(bmi(current, request.getHeightCm()))
                .waistChangeCm(change(sorted, MeasurementInput::getWaistCm))
                .bodyFatChangePercent(change(sorted, MeasurementInput::getBodyFatPercent))
                .daysTracked(daysTracked)
                .build();

        ProgressStatus status;
        if (Math.abs(current - target) <= GOAL_TOLERANCE_KG) {
            status = ProgressStatus.GOAL_REACHED;
        } else if (sorted.size() < 2 || daysTracked < MIN_DAYS_FOR_TREND) {
            status = ProgressStatus.INSUFFICIENT_DATA;
        } else {
            double weekly = round(weeklyTrend(sorted), 2);
            stats.setWeeklyTrendKg(weekly);
            status = classifyTrend(weekly, target - current);

            if (status == ProgressStatus.PROGRESSING) {
                double weeksToGoal = Math.abs(target - current) / Math.abs(weekly);
                stats.setEstimatedGoalDate(last.getMeasuredOn().plusDays(Math.round(weeksToGoal * 7)));
                if (weeksToGoal > LONG_ESTIMATE_WEEKS) {
                    warnings.add("At the current pace the goal is more than 2 years away. Consider reviewing your plan.");
                }
            }
            if (Math.abs(weekly) > current * MAX_SAFE_WEEKLY_RATE) {
                warnings.add("Your weight changes by more than 1% of body weight per week. A slower pace is safer and easier to maintain.");
            }
        }

        applyTargetDate(request.getTargetDate(), last.getMeasuredOn(), current, target, status, stats, warnings);

        return ProgressAnalysisResponse.builder()
                .status(status)
                .message(message(status, stats))
                .stats(stats)
                .points(sorted.stream().map(m -> toPoint(m, request.getHeightCm())).toList())
                .warnings(warnings)
                .build();
    }

    /** Least-squares slope of weight over time, converted to kg per week. */
    double weeklyTrend(List<MeasurementInput> sorted) {
        LocalDate origin = sorted.get(0).getMeasuredOn();
        int n = sorted.size();
        double meanX = 0;
        double meanY = 0;
        for (MeasurementInput m : sorted) {
            meanX += ChronoUnit.DAYS.between(origin, m.getMeasuredOn());
            meanY += m.getWeightKg();
        }
        meanX /= n;
        meanY /= n;

        double covariance = 0;
        double variance = 0;
        for (MeasurementInput m : sorted) {
            double dx = ChronoUnit.DAYS.between(origin, m.getMeasuredOn()) - meanX;
            covariance += dx * (m.getWeightKg() - meanY);
            variance += dx * dx;
        }
        return variance == 0 ? 0 : covariance / variance * 7;
    }

    private ProgressStatus classifyTrend(double weekly, double distanceToTarget) {
        if (Math.abs(weekly) < STABLE_THRESHOLD_KG_PER_WEEK) {
            return ProgressStatus.STABLE;
        }
        return Math.signum(weekly) == Math.signum(distanceToTarget)
                ? ProgressStatus.PROGRESSING
                : ProgressStatus.MOVING_AWAY;
    }

    private void applyTargetDate(LocalDate targetDate, LocalDate lastDate, double current, double target,
                                 ProgressStatus status, ProgressStats stats, List<String> warnings) {
        if (targetDate == null || status == ProgressStatus.GOAL_REACHED) {
            stats.setOnTrack(status == ProgressStatus.GOAL_REACHED ? true : null);
            return;
        }

        double weeksLeft = ChronoUnit.DAYS.between(lastDate, targetDate) / 7.0;
        double required = (target - current) / weeksLeft;
        stats.setRequiredWeeklyKg(round(required, 2));
        if (Math.abs(required) > current * MAX_SAFE_WEEKLY_RATE) {
            warnings.add("Reaching the goal by the target date would require more than 1% of body weight per week. Consider a later date.");
        }

        stats.setOnTrack(switch (status) {
            case PROGRESSING -> !stats.getEstimatedGoalDate().isAfter(targetDate);
            case STABLE, MOVING_AWAY -> false;
            default -> null;
        });
    }

    private String message(ProgressStatus status, ProgressStats stats) {
        return switch (status) {
            case GOAL_REACHED -> "Goal reached. Focus on maintaining your current weight.";
            case INSUFFICIENT_DATA -> "Add measurements over at least " + MIN_DAYS_FOR_TREND
                    + " days to calculate a reliable trend.";
            case STABLE -> "Your weight is stable. Adjust your plan if you want to move towards your goal.";
            case MOVING_AWAY -> "Your weight is moving away from your goal (" + formatSigned(stats.getWeeklyTrendKg())
                    + " kg per week).";
            case PROGRESSING -> "You are progressing at " + formatSigned(stats.getWeeklyTrendKg())
                    + " kg per week. Estimated goal date: " + stats.getEstimatedGoalDate() + ".";
        };
    }

    private int percentToGoal(double start, double current, double target) {
        if (Math.abs(current - target) <= GOAL_TOLERANCE_KG) {
            return 100;
        }
        double total = target - start;
        if (Math.abs(total) <= GOAL_TOLERANCE_KG) {
            return 0;
        }
        double percent = (current - start) / total * 100;
        return (int) Math.round(Math.max(0, Math.min(100, percent)));
    }

    private Double change(List<MeasurementInput> sorted, Function<MeasurementInput, Double> field) {
        List<Double> values = sorted.stream().map(field).filter(Objects::nonNull).toList();
        return values.size() < 2 ? null : round(values.get(values.size() - 1) - values.get(0), 1);
    }

    private ProgressPoint toPoint(MeasurementInput m, double heightCm) {
        return ProgressPoint.builder()
                .date(m.getMeasuredOn())
                .weightKg(m.getWeightKg())
                .waistCm(m.getWaistCm())
                .bodyFatPercent(m.getBodyFatPercent())
                .bmi(bmi(m.getWeightKg(), heightCm))
                .notes(m.getNotes())
                .build();
    }

    private double bmi(double weightKg, double heightCm) {
        double heightM = heightCm / 100.0;
        return round(weightKg / (heightM * heightM), 1);
    }

    private String formatSigned(double value) {
        return (value > 0 ? "+" : "") + value;
    }

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
