package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryDataValidatorTest {

    @Test
    void preservesMissingSleep() {
        RecoveryDataValidator.NormalizedSleep result = RecoveryDataValidator.normalizeSleep(null);
        assertThat(result.hours()).isNull();
        assertThat(result.notes()).isEmpty();
    }

    @Test
    void convertsMinuteSleepWithoutInventingANewReading() {
        RecoveryDataValidator.NormalizedSleep result = RecoveryDataValidator.normalizeSleep(480.0);
        assertThat(result.hours()).isEqualTo(8.0);
        assertThat(result.notes()).isNotEmpty();
    }

    @Test
    void rejectsFutureCheckinDates() {
        assertThatThrownBy(() -> RecoveryDataValidator.rejectFutureDate(LocalDate.now().plusDays(1), LocalDate.now()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("future");
    }

    @Test
    void flagsContradictorySleepAndNotes() {
        List<String> contradictions = RecoveryDataValidator.detectSubjectiveContradictions(
                3.0, 2, 2, "I slept well", null);
        assertThat(contradictions).isNotEmpty();
    }

    @Test
    void computesMedianWithoutGuessingMissingValues() {
        assertThat(RecoveryDataValidator.median(List.of())).isNull();
        assertThat(RecoveryDataValidator.median(List.of(50, 52, 60))).isEqualTo(52.0);
    }
}
