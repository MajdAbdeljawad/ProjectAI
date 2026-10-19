package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecoveryOverviewResponse;
import com.fitcoach.app.backend.entity.RecoveryCheckin;
import com.fitcoach.app.backend.entity.StravaActivity;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.entity.WorkoutDay;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import com.fitcoach.app.backend.entity.WorkoutWeek;
import com.fitcoach.app.backend.repository.RecoveryCheckinRepository;
import com.fitcoach.app.backend.repository.StravaActivityRepository;
import com.fitcoach.app.backend.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecoveryDataPreparationService {

    static final int CHECKIN_LOOKBACK_DAYS = 30;
    static final int TRAINING_LOOKBACK_DAYS = 14;
    static final int SLEEP_FRESH_HOURS = 36;
    static final int SUBJECTIVE_FRESH_HOURS = 36;
    static final int RHR_FRESH_HOURS = 48;
    static final int TRAINING_FRESH_HOURS = 72;

    private final RecoveryCheckinRepository recoveryCheckinRepository;
    private final StravaActivityRepository stravaActivityRepository;
    private final WorkoutPlanRepository workoutPlanRepository;

    @Transactional(readOnly = true)
    public RecoveryDataset prepare(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<String> missing = new ArrayList<>();
        List<String> quality = new ArrayList<>();
        List<String> contradictions = new ArrayList<>();
        List<String> safetyFlags = new ArrayList<>();

        List<RecoveryCheckin> checkins = recoveryCheckinRepository
                .findByUserIdAndRecordedOnGreaterThanEqualOrderByRecordedOnAscRecordedAtAsc(
                        user.getId(), today.minusDays(CHECKIN_LOOKBACK_DAYS));
        List<StravaActivity> activities = stravaActivityRepository.findByUserIdOrderByStartDateDesc(user.getId());
        WorkoutPlan plan = workoutPlanRepository.findFirstByUserIdOrderByGeneratedAtDesc(user.getId()).orElse(null);
        if (plan != null) {
            plan.getWeeks().forEach(week -> week.getDays().size());
        }

        RecoveryOverviewResponse.ProfileIndicator profile = profileIndicator(user.getProfile(), missing);
        RecoveryOverviewResponse.TrainingIndicator training = trainingIndicator(activities, user.getProfile(), now, missing, quality);
        RecoveryOverviewResponse.SleepIndicator sleep = sleepIndicator(checkins, now, missing, quality);
        RecoveryOverviewResponse.SubjectiveIndicator subjective = subjectiveIndicator(checkins, now, missing, quality);
        RecoveryOverviewResponse.HeartRateIndicator heartRate = heartRateIndicator(checkins, now, missing, quality);
        RecoveryOverviewResponse.UpcomingSessionIndicator upcoming = upcomingSession(plan, today, missing);
        RecoveryOverviewResponse.SymptomIndicator symptoms = symptomIndicator(checkins, now, missing, quality);

        RecoveryCheckin latest = latestCheckin(checkins);
        if (latest != null) {
            contradictions.addAll(RecoveryDataValidator.detectSubjectiveContradictions(
                    latest.getSleepHours(),
                    latest.getFatigueLevel(),
                    latest.getSorenessLevel(),
                    latest.getNotes(),
                    latest.getSymptoms()
            ));
            quality.addAll(RecoveryDataValidator.inspectHeartRate(latest.getRestingHeartRate()));
        }

        boolean concerning = RecoverySafetyPolicy.mentionsConcerningSymptoms(combinedSymptomText(symptoms));
        boolean significantPain = RecoverySafetyPolicy.mentionsSignificantPain(combinedSymptomText(symptoms))
                || (subjective.getSorenessLevel() != null && subjective.getSorenessLevel() >= 8);
        if (concerning) {
            safetyFlags.add("Concerning symptoms were reported.");
        }
        if (significantPain) {
            safetyFlags.add("Significant pain or very high soreness was reported.");
        }
        if (!contradictions.isEmpty()) {
            quality.add("Contradictory inputs were preserved and flagged rather than resolved automatically.");
        }

        boolean minimumSignal = sleep.isAvailable() || subjective.isAvailable() || heartRate.isAvailable()
                || training.isAvailable() || symptoms.isAvailable();

        List<Integer> ages = new ArrayList<>();
        ages.add(ageHours(sleep.getLatestDate(), now));
        ages.add(ageHours(subjective.getLatestDate(), now));
        ages.add(ageHours(heartRate.getLatestDate(), now));
        if (training.getLatestSessionAt() != null) {
            ages.add((int) ChronoUnit.HOURS.between(training.getLatestSessionAt(), now));
        }
        Integer oldest = oldestAgeHours(ages);

        return RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder()
                        .profile(profile)
                        .training(training)
                        .sleep(sleep)
                        .subjective(subjective)
                        .heartRate(heartRate)
                        .upcomingSession(upcoming)
                        .symptoms(symptoms)
                        .build())
                .missingFields(missing)
                .dataQualityNotes(quality.stream().distinct().toList())
                .contradictions(contradictions.stream().distinct().toList())
                .safetyFlags(safetyFlags.stream().distinct().toList())
                .oldestInputAgeHours(oldest)
                .minimumSignalPresent(minimumSignal)
                .concerningSymptomsPresent(concerning)
                .significantPainPresent(significantPain)
                .build();
    }

    private RecoveryOverviewResponse.ProfileIndicator profileIndicator(UserProfile profile, List<String> missing) {
        if (profile == null) {
            missing.add("User profile");
            return RecoveryOverviewResponse.ProfileIndicator.builder().available(false).freshness("missing").build();
        }
        if (profile.getMainGoal() == null || profile.getMainGoal().isBlank()) {
            missing.add("Fitness goal");
        }
        return RecoveryOverviewResponse.ProfileIndicator.builder()
                .firstName(profile.getFirstName())
                .mainGoal(profile.getMainGoal())
                .fitnessLevel(profile.getFitnessLevel())
                .trainingDaysPerWeek(profile.getTrainingDaysPerWeek())
                .sessionDurationMinutes(profile.getSessionDurationMinutes())
                .available(true)
                .freshness("available")
                .build();
    }

    private RecoveryOverviewResponse.TrainingIndicator trainingIndicator(List<StravaActivity> activities,
                                                                        UserProfile profile,
                                                                        LocalDateTime now,
                                                                        List<String> missing,
                                                                        List<String> quality) {
        LocalDateTime from = now.minusDays(7);
        List<StravaActivity> recent = activities.stream()
                .filter(activity -> activity.getStartDate() != null && !activity.getStartDate().isAfter(now)
                        && activity.getStartDate().isAfter(now.minusDays(TRAINING_LOOKBACK_DAYS)))
                .toList();
        List<StravaActivity> last7 = recent.stream()
                .filter(activity -> activity.getStartDate().isAfter(from))
                .toList();

        if (last7.isEmpty()) {
            missing.add("Recent training sessions");
            return RecoveryOverviewResponse.TrainingIndicator.builder()
                    .recentSessionCount(0)
                    .totalMinutes7d(0L)
                    .available(false)
                    .freshness("missing")
                    .build();
        }

        long minutes = last7.stream()
                .mapToLong(activity -> activity.getDurationSeconds() == null ? 0 : activity.getDurationSeconds() / 60)
                .sum();
        List<Double> heartRates = last7.stream()
                .map(StravaActivity::getAverageHeartRate)
                .filter(Objects::nonNull)
                .toList();
        Double averageHr = heartRates.isEmpty() ? null : heartRates.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        if (averageHr != null && averageHr.isNaN()) {
            averageHr = null;
        }
        if (heartRates.size() < last7.size()) {
            quality.add("Some recent sessions have no heart-rate data. Intensity was not inferred for those sessions.");
        }

        LocalDateTime latest = last7.stream().map(StravaActivity::getStartDate).max(LocalDateTime::compareTo).orElse(null);
        int ageHours = latest == null ? TRAINING_FRESH_HOURS + 1 : (int) ChronoUnit.HOURS.between(latest, now);
        String intensity = intensityLabel(minutes, last7.size(), averageHr, profile);

        return RecoveryOverviewResponse.TrainingIndicator.builder()
                .recentSessionCount(last7.size())
                .totalMinutes7d(minutes)
                .averageHeartRate(averageHr)
                .intensityLabel(intensity)
                .latestSessionAt(latest)
                .available(true)
                .freshness(RecoveryDataValidator.freshnessLabel(ageHours, TRAINING_FRESH_HOURS, 168))
                .build();
    }

    private String intensityLabel(long minutes, int sessions, Double averageHr, UserProfile profile) {
        Integer plannedDays = profile == null ? null : profile.getTrainingDaysPerWeek();
        Integer plannedMinutes = profile == null ? null : profile.getSessionDurationMinutes();
        String volume;
        if (plannedDays != null && plannedMinutes != null && plannedDays > 0) {
            long planned = (long) plannedDays * plannedMinutes;
            if (minutes > planned * 1.3) {
                volume = "high volume versus the user's planned week";
            } else if (minutes < planned * 0.5) {
                volume = "low volume versus the user's planned week";
            } else {
                volume = "moderate volume versus the user's planned week";
            }
        } else if (minutes >= 300 || sessions >= 6) {
            volume = "high recent duration";
        } else if (minutes <= 60) {
            volume = "low recent duration";
        } else {
            volume = "moderate recent duration";
        }
        if (averageHr == null) {
            return volume + "; session heart rate unavailable";
        }
        return volume + "; average activity heart rate " + Math.round(averageHr) + " bpm where recorded";
    }

    private RecoveryOverviewResponse.SleepIndicator sleepIndicator(List<RecoveryCheckin> checkins,
                                                                   LocalDateTime now,
                                                                   List<String> missing,
                                                                   List<String> quality) {
        List<RecoveryCheckin> withSleep = latestPerDay(checkins.stream()
                .filter(checkin -> checkin.getSleepHours() != null)
                .toList());
        if (withSleep.isEmpty()) {
            missing.add("Sleep duration");
            return RecoveryOverviewResponse.SleepIndicator.builder().available(false).freshness("missing").build();
        }
        RecoveryCheckin latest = withSleep.get(withSleep.size() - 1);
        int ageHours = ageHours(latest.getRecordedOn(), now);
        if (ageHours > SLEEP_FRESH_HOURS) {
            quality.add("Latest sleep reading is " + ageHours + " hours old.");
        }
        List<Double> last7 = withSleep.stream()
                .filter(checkin -> !checkin.getRecordedOn().isBefore(now.toLocalDate().minusDays(6)))
                .map(RecoveryCheckin::getSleepHours)
                .toList();
        Double average = last7.isEmpty() ? null : last7.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        if (average != null && average.isNaN()) {
            average = null;
        }
        String trend = "unknown";
        if (withSleep.size() >= 3) {
            List<RecoveryCheckin> recent = withSleep.subList(Math.max(0, withSleep.size() - 3), withSleep.size());
            List<RecoveryCheckin> previous = withSleep.subList(0, Math.max(0, withSleep.size() - 3));
            if (!previous.isEmpty()) {
                double recentAvg = recent.stream().mapToDouble(RecoveryCheckin::getSleepHours).average().orElse(Double.NaN);
                double previousAvg = previous.stream().mapToDouble(RecoveryCheckin::getSleepHours).average().orElse(Double.NaN);
                trend = RecoveryDataValidator.trendFromAverages(recentAvg, previousAvg);
            }
        } else {
            quality.add("Fewer than 3 sleep readings are available, so a sleep trend was not computed.");
        }
        return RecoveryOverviewResponse.SleepIndicator.builder()
                .latestHours(latest.getSleepHours())
                .averageHours7d(average)
                .trend(trend)
                .latestDate(latest.getRecordedOn())
                .available(true)
                .freshness(RecoveryDataValidator.freshnessLabel(ageHours, SLEEP_FRESH_HOURS, 72))
                .build();
    }

    private RecoveryOverviewResponse.SubjectiveIndicator subjectiveIndicator(List<RecoveryCheckin> checkins,
                                                                            LocalDateTime now,
                                                                            List<String> missing,
                                                                            List<String> quality) {
        RecoveryCheckin latest = latestCheckin(checkins.stream()
                .filter(checkin -> checkin.getFatigueLevel() != null || checkin.getSorenessLevel() != null)
                .toList());
        if (latest == null) {
            missing.add("Self-reported fatigue and soreness");
            return RecoveryOverviewResponse.SubjectiveIndicator.builder().available(false).freshness("missing").build();
        }
        if (latest.getFatigueLevel() == null) {
            missing.add("Fatigue rating");
        }
        if (latest.getSorenessLevel() == null) {
            missing.add("Soreness rating");
        }
        int ageHours = ageHours(latest.getRecordedOn(), now);
        if (ageHours > SUBJECTIVE_FRESH_HOURS) {
            quality.add("Latest fatigue/soreness check-in is " + ageHours + " hours old.");
        }
        return RecoveryOverviewResponse.SubjectiveIndicator.builder()
                .fatigueLevel(latest.getFatigueLevel())
                .sorenessLevel(latest.getSorenessLevel())
                .sorenessAreas(latest.getSorenessAreas())
                .latestDate(latest.getRecordedOn())
                .available(true)
                .freshness(RecoveryDataValidator.freshnessLabel(ageHours, SUBJECTIVE_FRESH_HOURS, 72))
                .build();
    }

    private RecoveryOverviewResponse.HeartRateIndicator heartRateIndicator(List<RecoveryCheckin> checkins,
                                                                           LocalDateTime now,
                                                                           List<String> missing,
                                                                           List<String> quality) {
        List<RecoveryCheckin> withRhr = latestPerDay(checkins.stream()
                .filter(checkin -> checkin.getRestingHeartRate() != null)
                .toList());
        if (withRhr.isEmpty()) {
            missing.add("Resting heart rate");
            missing.add("Personal resting heart-rate baseline");
            return RecoveryOverviewResponse.HeartRateIndicator.builder()
                    .available(false)
                    .baselineAvailable(false)
                    .freshness("missing")
                    .build();
        }
        RecoveryCheckin latest = withRhr.get(withRhr.size() - 1);
        int ageHours = ageHours(latest.getRecordedOn(), now);
        Double baseline = null;
        if (withRhr.size() >= 5) {
            List<Integer> baselineValues = withRhr.subList(0, withRhr.size() - 1).stream()
                    .map(RecoveryCheckin::getRestingHeartRate)
                    .toList();
            baseline = RecoveryDataValidator.median(baselineValues);
        } else {
            missing.add("Personal resting heart-rate baseline");
            quality.add("Fewer than 5 resting heart-rate readings are available, so no personal baseline was computed.");
        }
        Double delta = baseline == null ? null : latest.getRestingHeartRate() - baseline;
        return RecoveryOverviewResponse.HeartRateIndicator.builder()
                .latestRestingHeartRate(latest.getRestingHeartRate())
                .personalBaseline(baseline)
                .deltaFromBaseline(delta)
                .sampleCount(withRhr.size())
                .latestDate(latest.getRecordedOn())
                .available(true)
                .baselineAvailable(baseline != null)
                .freshness(RecoveryDataValidator.freshnessLabel(ageHours, RHR_FRESH_HOURS, 96))
                .build();
    }

    private RecoveryOverviewResponse.UpcomingSessionIndicator upcomingSession(WorkoutPlan plan,
                                                                             LocalDate today,
                                                                             List<String> missing) {
        if (plan == null || plan.getWeeks() == null || plan.getWeeks().isEmpty()) {
            missing.add("Upcoming training session");
            return RecoveryOverviewResponse.UpcomingSessionIndicator.builder().available(false).freshness("missing").build();
        }
        LocalDate start = plan.getGeneratedAt() == null ? today : plan.getGeneratedAt().toLocalDate();
        long offset = ChronoUnit.DAYS.between(start, today);
        if (offset < 0) {
            offset = 0;
        }
        if (offset >= plan.getDurationDays()) {
            missing.add("Upcoming training session");
            return RecoveryOverviewResponse.UpcomingSessionIndicator.builder().available(false).freshness("missing").build();
        }

        WorkoutDay selected = findPlannedDay(plan, offset);
        if (selected == null) {
            missing.add("Upcoming training session");
            return RecoveryOverviewResponse.UpcomingSessionIndicator.builder().available(false).freshness("missing").build();
        }
        boolean rest = selected.getType() != null && selected.getType().toUpperCase().contains("REST");
        if (rest) {
            WorkoutDay next = findNextTrainingDay(plan, offset + 1);
            if (next != null) {
                selected = next;
                rest = false;
                offset = ((long) (weekNumber(plan, next) - 1) * 7) + next.getDayNumber() - 1;
            }
        }
        return RecoveryOverviewResponse.UpcomingSessionIndicator.builder()
                .weekNumber(weekNumber(plan, selected))
                .dayNumber(selected.getDayNumber())
                .type(selected.getType())
                .notes(selected.getNotes())
                .plannedDate(start.plusDays(Math.max(offset, 0)))
                .restDay(rest)
                .available(true)
                .freshness("available")
                .build();
    }

    private RecoveryOverviewResponse.SymptomIndicator symptomIndicator(List<RecoveryCheckin> checkins,
                                                                      LocalDateTime now,
                                                                      List<String> missing,
                                                                      List<String> quality) {
        RecoveryCheckin latest = latestCheckin(checkins.stream()
                .filter(checkin -> blankToNull(checkin.getSymptoms()) != null || blankToNull(checkin.getLimitations()) != null)
                .toList());
        String symptoms = latest == null ? null : blankToNull(latest.getSymptoms());
        String limitations = latest == null ? null : blankToNull(latest.getLimitations());
        if (symptoms == null && limitations == null) {
            missing.add("Reported symptoms and limitations");
            return RecoveryOverviewResponse.SymptomIndicator.builder().available(false).freshness("missing").build();
        }
        int ageHours = latest == null ? 0 : ageHours(latest.getRecordedOn(), now);
        if (ageHours > SUBJECTIVE_FRESH_HOURS) {
            quality.add("Symptom/limitation report is " + ageHours + " hours old.");
        }
        return RecoveryOverviewResponse.SymptomIndicator.builder()
                .symptoms(symptoms)
                .limitations(limitations)
                .latestDate(latest == null ? null : latest.getRecordedOn())
                .available(true)
                .freshness(RecoveryDataValidator.freshnessLabel(ageHours, SUBJECTIVE_FRESH_HOURS, 72))
                .build();
    }

    private List<RecoveryCheckin> latestPerDay(List<RecoveryCheckin> checkins) {
        return checkins.stream()
                .collect(java.util.stream.Collectors.groupingBy(RecoveryCheckin::getRecordedOn))
                .values()
                .stream()
                .map(dayGroup -> dayGroup.stream().max(Comparator.comparing(RecoveryCheckin::getRecordedAt)).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecoveryCheckin::getRecordedOn).thenComparing(RecoveryCheckin::getRecordedAt))
                .toList();
    }

    private RecoveryCheckin latestCheckin(List<RecoveryCheckin> checkins) {
        return checkins.stream()
                .max(Comparator.comparing(RecoveryCheckin::getRecordedOn).thenComparing(RecoveryCheckin::getRecordedAt))
                .orElse(null);
    }

    private WorkoutDay findPlannedDay(WorkoutPlan plan, long dayOffset) {
        int weekNumber = (int) (dayOffset / 7) + 1;
        int dayNumber = (int) (dayOffset % 7) + 1;
        return plan.getWeeks().stream()
                .filter(week -> week.getWeekNumber() == weekNumber)
                .flatMap(week -> week.getDays().stream())
                .filter(day -> day.getDayNumber() == dayNumber)
                .findFirst()
                .orElse(null);
    }

    private WorkoutDay findNextTrainingDay(WorkoutPlan plan, long fromOffset) {
        for (long offset = fromOffset; offset < plan.getDurationDays(); offset++) {
            WorkoutDay day = findPlannedDay(plan, offset);
            if (day != null && (day.getType() == null || !day.getType().toUpperCase().contains("REST"))) {
                return day;
            }
        }
        return null;
    }

    private int weekNumber(WorkoutPlan plan, WorkoutDay day) {
        WorkoutWeek week = day.getWeek();
        if (week != null) {
            return week.getWeekNumber();
        }
        return plan.getWeeks().stream()
                .filter(candidate -> candidate.getDays().contains(day))
                .map(WorkoutWeek::getWeekNumber)
                .findFirst()
                .orElse(1);
    }

    private int ageHours(LocalDate date, LocalDateTime now) {
        if (date == null) {
            return Integer.MAX_VALUE;
        }
        return (int) ChronoUnit.HOURS.between(date.atStartOfDay(), now);
    }

    private Integer oldestAgeHours(List<Integer> ages) {
        return ages.stream().filter(Objects::nonNull).filter(age -> age < Integer.MAX_VALUE / 4).max(Integer::compareTo).orElse(null);
    }

    private String combinedSymptomText(RecoveryOverviewResponse.SymptomIndicator symptoms) {
        if (symptoms == null) {
            return "";
        }
        return safe(symptoms.getSymptoms()) + " " + safe(symptoms.getLimitations());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
