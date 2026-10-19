package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.RecoveryCheckin;
import com.fitcoach.app.backend.entity.StravaActivity;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.repository.RecoveryCheckinRepository;
import com.fitcoach.app.backend.repository.StravaActivityRepository;
import com.fitcoach.app.backend.repository.WorkoutPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryDataPreparationServiceTest {

    @Mock
    private RecoveryCheckinRepository recoveryCheckinRepository;
    @Mock
    private StravaActivityRepository stravaActivityRepository;
    @Mock
    private WorkoutPlanRepository workoutPlanRepository;

    private RecoveryDataPreparationService service;

    @BeforeEach
    void setUp() {
        service = new RecoveryDataPreparationService(
                recoveryCheckinRepository, stravaActivityRepository, workoutPlanRepository);
    }

    @Test
    void preservesMissingSleepHeartRateAndStravaInsteadOfInventingThem() {
        User user = userWithProfile();
        when(recoveryCheckinRepository.findByUserIdAndRecordedOnGreaterThanEqualOrderByRecordedOnAscRecordedAtAsc(eq(1L), any()))
                .thenReturn(List.of());
        when(stravaActivityRepository.findByUserIdOrderByStartDateDesc(1L)).thenReturn(List.of());
        when(workoutPlanRepository.findFirstByUserIdOrderByGeneratedAtDesc(1L)).thenReturn(Optional.empty());

        RecoveryDataset dataset = service.prepare(user);

        assertThat(dataset.getIndicators().getSleep().isAvailable()).isFalse();
        assertThat(dataset.getIndicators().getSleep().getLatestHours()).isNull();
        assertThat(dataset.getIndicators().getHeartRate().isAvailable()).isFalse();
        assertThat(dataset.getIndicators().getHeartRate().getLatestRestingHeartRate()).isNull();
        assertThat(dataset.getIndicators().getTraining().isAvailable()).isFalse();
        assertThat(dataset.getMissingFields()).contains("Sleep duration", "Resting heart rate", "Recent training sessions");
        assertThat(dataset.hasMinimumSignal()).isFalse();
    }

    @Test
    void usesReportedSleepAndDoesNotFillMissingHeartRate() {
        User user = userWithProfile();
        RecoveryCheckin checkin = new RecoveryCheckin();
        checkin.setRecordedOn(LocalDate.now());
        checkin.setRecordedAt(LocalDateTime.now());
        checkin.setSleepHours(7.0);
        checkin.setFatigueLevel(4);
        checkin.setSorenessLevel(3);

        StravaActivity activity = new StravaActivity();
        activity.setType("Run");
        activity.setStartDate(LocalDateTime.now().minusHours(5));
        activity.setDurationSeconds(2400L);
        activity.setAverageHeartRate(null);

        when(recoveryCheckinRepository.findByUserIdAndRecordedOnGreaterThanEqualOrderByRecordedOnAscRecordedAtAsc(eq(1L), any()))
                .thenReturn(List.of(checkin));
        when(stravaActivityRepository.findByUserIdOrderByStartDateDesc(1L)).thenReturn(List.of(activity));
        when(workoutPlanRepository.findFirstByUserIdOrderByGeneratedAtDesc(1L)).thenReturn(Optional.empty());

        RecoveryDataset dataset = service.prepare(user);

        assertThat(dataset.getIndicators().getSleep().getLatestHours()).isEqualTo(7.0);
        assertThat(dataset.getIndicators().getHeartRate().getLatestRestingHeartRate()).isNull();
        assertThat(dataset.getIndicators().getTraining().getAverageHeartRate()).isNull();
        assertThat(dataset.getIndicators().getTraining().getIntensityLabel()).contains("heart rate unavailable");
        assertThat(dataset.hasMinimumSignal()).isTrue();
    }

    @Test
    void keepsEarlierSameDayMetricsWhenALaterCheckinOmitsThem() {
        User user = userWithProfile();
        RecoveryCheckin morning = new RecoveryCheckin();
        morning.setRecordedOn(LocalDate.now());
        morning.setRecordedAt(LocalDateTime.now().minusHours(2));
        morning.setSleepHours(7.2);
        morning.setRestingHeartRate(58);
        morning.setFatigueLevel(4);

        RecoveryCheckin later = new RecoveryCheckin();
        later.setRecordedOn(LocalDate.now());
        later.setRecordedAt(LocalDateTime.now());
        later.setSymptoms("tight calves");

        when(recoveryCheckinRepository.findByUserIdAndRecordedOnGreaterThanEqualOrderByRecordedOnAscRecordedAtAsc(eq(1L), any()))
                .thenReturn(List.of(morning, later));
        when(stravaActivityRepository.findByUserIdOrderByStartDateDesc(1L)).thenReturn(List.of());
        when(workoutPlanRepository.findFirstByUserIdOrderByGeneratedAtDesc(1L)).thenReturn(Optional.empty());

        RecoveryDataset dataset = service.prepare(user);

        assertThat(dataset.getIndicators().getSleep().getLatestHours()).isEqualTo(7.2);
        assertThat(dataset.getIndicators().getHeartRate().getLatestRestingHeartRate()).isEqualTo(58);
        assertThat(dataset.getIndicators().getSubjective().getFatigueLevel()).isEqualTo(4);
        assertThat(dataset.getIndicators().getSymptoms().getSymptoms()).isEqualTo("tight calves");
    }

    private User userWithProfile() {
        User user = new User();
        user.setId(1L);
        user.setEmail("athlete@example.com");
        UserProfile profile = new UserProfile();
        profile.setFirstName("Ada");
        profile.setLastName("Lovelace");
        profile.setMainGoal("Build muscle");
        profile.setTrainingDaysPerWeek(4);
        profile.setSessionDurationMinutes(45);
        user.setProfile(profile);
        return user;
    }
}
