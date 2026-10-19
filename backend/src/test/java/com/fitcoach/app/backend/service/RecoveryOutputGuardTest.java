package com.fitcoach.app.backend.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryOutputGuardTest {

    @Test
    void rejectsDiagnosisClaims() {
        RecoveryPipelineModels.ContextAnalysis context = new RecoveryPipelineModels.ContextAnalysis(
                List.of(), List.of(), List.of(), List.of("Diagnosed pneumonia"), List.of(), "disclaimer");
        assertThatThrownBy(() -> RecoveryOutputGuard.assertNoDisallowedClaims(context))
                .isInstanceOf(RecoveryProviderException.class)
                .extracting(ex -> ((RecoveryProviderException) ex).getCode())
                .isEqualTo("MALFORMED_RESPONSE");
    }

    @Test
    void acceptsOrdinaryRecoveryLanguage() {
        RecoveryPipelineModels.Recommendation recommendation = new RecoveryPipelineModels.Recommendation(
                "LIGHT_RECOVERY",
                "Walk easily",
                "Sleep was short, so load should not increase.",
                List.of("Sleep opportunity"),
                "This is not a diagnosis.",
                false
        );
        assertThatCode(() -> RecoveryOutputGuard.assertNoDisallowedClaims(recommendation)).doesNotThrowAnyException();
    }
}
