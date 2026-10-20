package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.ProgressAnalysisRequest;
import com.fitcoach.app.backend.dto.ProgressAnalysisRequest.MeasurementInput;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressCalculationsServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);

    private final ProgressCalculationsService service = new ProgressCalculationsService();

    @Nested
    @DisplayName("Calcul de tendance")
    class TrendCalculation {

        @Test
        @DisplayName("perte régulière → PROGRESSING + date estimée")
        void lossTrendEstimatesGoalDate() {
            ProgressAnalysisResponse result = service.analyze(request(75.0,
                    m(0, 80.0), m(7, 79.5), m(14, 79.0), m(21, 78.5), m(28, 78.0)));

            assertThat(result.getStatus()).isEqualTo(ProgressStatus.PROGRESSING);
            assertThat(result.getStats().getWeeklyTrendKg()).isEqualTo(-0.5);
            assertThat(result.getStats().getChangeKg()).isEqualTo(-2.0);
            assertThat(result.getStats().getRemainingKg()).isEqualTo(3.0);
            assertThat(result.getStats().getPercentToGoal()).isEqualTo(40);
            assertThat(result.getStats().getEstimatedGoalDate()).isEqualTo(START.plusDays(28 + 42));
        }

        @Test
        @DisplayName("gain régulier vers un poids cible plus haut → PROGRESSING")
        void gainTrendEstimatesGoalDate() {
            ProgressAnalysisResponse result = service.analyze(request(72.0,
                    m(0, 70.0), m(14, 70.5), m(28, 71.0)));

            assertThat(result.getStatus()).isEqualTo(ProgressStatus.PROGRESSING);
            assertThat(result.getStats().getWeeklyTrendKg()).isEqualTo(0.25);
            assertThat(result.getStats().getChangeKg()).isEqualTo(1.0);
            assertThat(result.getStats().getEstimatedGoalDate()).isEqualTo(START.plusDays(28 + 28));
        }

        @Test
        @DisplayName("variations < 0,1 kg/semaine → STABLE")
        void smallFluctuationsAreStable() {
            ProgressAnalysisResponse result = service.analyze(request(75.0,
                    m(0, 80.0), m(7, 80.2), m(14, 79.9), m(21, 80.0)));

            assertThat(result.getStatus()).isEqualTo(ProgressStatus.STABLE);
            assertThat(result.getStats().getWeeklyTrendKg()).isBetween(-0.1, 0.1);
            assertThat(result.getStats().getEstimatedGoalDate()).isNull();
        }

        @Test
        @DisplayName("une seule mesure → INSUFFICIENT_DATA")
        void singleMeasurementGivesInsufficientData() {
            ProgressAnalysisResponse result = service.analyze(request(75.0, m(0, 80.0)));

            assertThat(result.getStatus()).isEqualTo(ProgressStatus.INSUFFICIENT_DATA);
            assertThat(result.getStats().getWeeklyTrendKg()).isNull();
            assertThat(result.getStats().getEstimatedGoalDate()).isNull();
            assertThat(result.getPoints()).hasSize(1);
            assertThat(result.getMessage()).contains("7");
        }

        @Test
        @DisplayName("poids à ±0,5 kg de la cible → GOAL_REACHED")
        void weightWithinToleranceIsGoalReached() {
            ProgressAnalysisResponse result = service.analyze(request(75.0, m(0, 80.0), m(60, 75.3)));

            assertThat(result.getStatus()).isEqualTo(ProgressStatus.GOAL_REACHED);
            assertThat(result.getStats().getPercentToGoal()).isEqualTo(100);
            assertThat(result.getStats().getRemainingKg()).isEqualTo(0.3);
        }

        @Test
        @DisplayName("tendance qui s'éloigne de l'objectif → MOVING_AWAY")
        void trendAwayFromGoalIsDetected() {
            ProgressAnalysisResponse result = service.analyze(request(75.0,
                    m(0, 80.0), m(7, 80.5), m(14, 81.0)));

            assertThat(result.getStatus()).isEqualTo(ProgressStatus.MOVING_AWAY);
            assertThat(result.getStats().getWeeklyTrendKg()).isPositive();
            assertThat(result.getStats().getEstimatedGoalDate()).isNull();
            assertThat(result.getStats().getPercentToGoal()).isZero();
        }
    }

    private MeasurementInput m(int day, double weightKg) {
        MeasurementInput measurement = new MeasurementInput();
        measurement.setMeasuredOn(START.plusDays(day));
        measurement.setWeightKg(weightKg);
        return measurement;
    }

    private ProgressAnalysisRequest request(double targetWeightKg, MeasurementInput... measurements) {
        ProgressAnalysisRequest request = new ProgressAnalysisRequest();
        request.setFirstName("Test");
        request.setHeightCm(178.0);
        request.setMainGoal("fat loss");
        request.setTargetWeightKg(targetWeightKg);
        request.setMeasurements(new ArrayList<>(List.of(measurements)));
        return request;
    }
}
