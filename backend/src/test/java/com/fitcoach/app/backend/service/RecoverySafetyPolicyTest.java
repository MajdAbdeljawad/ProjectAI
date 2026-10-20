package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecoveryOverviewResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecoverySafetyPolicyTest {

    @Test
    void escalatesConcerningSymptomsToRestAndProfessionalEvaluation() {
        RecoveryDataset dataset = RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder()
                        .symptoms(RecoveryOverviewResponse.SymptomIndicator.builder()
                                .symptoms("Chest pain during yesterday's run")
                                .available(true)
                                .build())
                        .build())
                .minimumSignalPresent(true)
                .build();

        RecoverySafetyPolicy.SafetyDecision decision = RecoverySafetyPolicy.evaluate(dataset);

        assertThat(decision.blocksTraining()).isTrue();
        assertThat(decision.requiresProfessionalEvaluation()).isTrue();
        assertThat(decision.forcedAction()).isEqualTo("REST");
    }

    @Test
    void doesNotRecommendContinuingThroughHighSoreness() {
        RecoveryDataset dataset = RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder()
                        .subjective(RecoveryOverviewResponse.SubjectiveIndicator.builder()
                                .sorenessLevel(9)
                                .available(true)
                                .build())
                        .build())
                .minimumSignalPresent(true)
                .build();

        RecoverySafetyPolicy.SafetyDecision decision = RecoverySafetyPolicy.evaluate(dataset);
        assertThat(RecoverySafetyPolicy.overrideAction("CONTINUE_PLAN", decision)).isEqualTo("REST");
    }
}
