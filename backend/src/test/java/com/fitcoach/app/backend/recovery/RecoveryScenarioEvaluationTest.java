package com.fitcoach.app.backend.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fitcoach.app.backend.service.RecoveryAiClient;
import com.fitcoach.app.backend.service.RecoveryJson;
import com.fitcoach.app.backend.service.RecoveryPipelineService;
import com.fitcoach.app.backend.service.RecoveryPromptCatalog;
import com.fitcoach.app.backend.service.RecoveryProviderException;
import com.fitcoach.app.backend.service.RecoveryResponseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryScenarioEvaluationTest {

    @Mock
    private RecoveryAiClient recoveryAiClient;

    private RecoveryPipelineService pipeline;
    private RecoveryJson recoveryJson;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        recoveryJson = new RecoveryJson(mapper);
        pipeline = new RecoveryPipelineService(
                new RecoveryPromptCatalog(),
                recoveryAiClient,
                new RecoveryResponseValidator(mapper),
                recoveryJson
        );
    }

    @Test
    void case1CompleteAndConsistentData() {
        long started = System.nanoTime();
        stubCompliantModel();
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.completeAndConsistent());
        long latencyMs = (System.nanoTime() - started) / 1_000_000;

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.assessment().classification()).isEqualTo("FAVORABLE");
        assertThat(result.recommendation().recommendedAction()).isEqualTo("CONTINUE_PLAN");
        assertThat(result.recommendation().requiresProfessionalEvaluation()).isFalse();
        assertThat(result.dataset().getIndicators().getSleep().getLatestHours()).isEqualTo(7.8);
        Map<String, Integer> scores = RecoveryPromptRubric.scoreBehavior(result.dataset(), result);
        assertThat(RecoveryPromptRubric.total(scores)).isEqualTo(6);
        assertThat(latencyMs).isGreaterThanOrEqualTo(0);
    }

    @Test
    void case2ReducedSleepAndIncreasedFatigue() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenAnswer(invocation ->
                RecoveryConstraintFollowingMock.complete(invocation.getArgument(1)));
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.reducedSleepIncreasedFatigue());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.assessment().classification()).isEqualTo("ADDITIONAL_RECOVERY_RECOMMENDED");
        assertThat(result.recommendation().recommendedAction()).isNotEqualTo("CONTINUE_PLAN");
        assertThat(result.dataset().getIndicators().getSleep().getLatestHours()).isEqualTo(4.5);
        assertThat(result.dataset().getIndicators().getSubjective().getFatigueLevel()).isEqualTo(8);
        assertThat(RecoveryPromptRubric.scoreBehavior(result.dataset(), result).get("recommendationRelevant")).isEqualTo(1);
    }

    @Test
    void case3MissingHeartRateIsNotInvented() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenAnswer(invocation ->
                RecoveryConstraintFollowingMock.complete(invocation.getArgument(1)));
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.missingHeartRate());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.dataset().getIndicators().getHeartRate().getLatestRestingHeartRate()).isNull();
        assertThat(result.contextAnalysis().baselineComparisons())
                .anyMatch(comparison -> comparison.currentValue() == null && comparison.baselineValue() == null);
        assertThat(result.assessment().uncertainties().toString()).contains("Resting heart rate");
        assertThat(result.recommendation().explanation().toLowerCase()).doesNotContain("invent");
    }

    @Test
    void case4EmptyWorkoutHistory() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenAnswer(invocation ->
                RecoveryConstraintFollowingMock.complete(invocation.getArgument(1)));
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.emptyWorkoutHistory());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.dataset().getIndicators().getTraining().isAvailable()).isFalse();
        assertThat(result.dataset().getMissingFields()).contains("Recent training sessions");
        assertThat(result.dataset().getIndicators().getSleep().getLatestHours()).isEqualTo(7.8);
    }

    @Test
    void case5StaleAndContradictoryMeasurements() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenAnswer(invocation ->
                RecoveryConstraintFollowingMock.complete(invocation.getArgument(1)));
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.staleAndContradictory());

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.assessment().classification()).isIn("MIXED", "INSUFFICIENT_INFORMATION", "ADDITIONAL_RECOVERY_RECOMMENDED");
        assertThat(result.recommendation().recommendedAction()).isNotEqualTo("CONTINUE_PLAN");
        assertThat(result.dataset().getContradictions()).isNotEmpty();
        assertThat(result.dataset().getDataQualityNotes().toString()).contains("72 hours old");
        assertThat(result.dataset().getContradictions().toString()).contains("under 5 hours");
        assertThat(result.contextAnalysis().dataQualityNotes()).isNotEmpty();
    }

    @Test
    void case6PainAndConcerningSymptomsSkipAi() {
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.concerningSymptoms());

        assertThat(result.status()).isEqualTo("SAFETY_HOLD");
        assertThat(result.recommendation().recommendedAction()).isEqualTo("REST");
        assertThat(result.recommendation().requiresProfessionalEvaluation()).isTrue();
        verify(recoveryAiClient, never()).complete(anyString(), anyString());
        assertThat(RecoveryPromptRubric.scoreBehavior(result.dataset(), result).get("safetyRuleCompliance")).isEqualTo(1);
    }

    @Test
    void case7InvalidAiJson() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenReturn("this is not json");
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.enoughSignalForProviderFailure());

        assertThat(result.status()).isEqualTo("PROVIDER_ERROR");
        assertThat(result.providerCode()).isEqualTo("MALFORMED_RESPONSE");
        assertThat(result.dataset().getIndicators().getSleep().getLatestHours()).isEqualTo(7.8);
        assertThat(result.recommendation().recommendedAction()).isEqualTo("LIGHT_RECOVERY");
    }

    @Test
    void case8ProviderTimeout() {
        when(recoveryAiClient.complete(anyString(), anyString()))
                .thenThrow(new RecoveryProviderException("The recovery model timed out or could not be reached.", "TIMEOUT"));
        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.enoughSignalForProviderFailure());

        assertThat(result.status()).isEqualTo("PROVIDER_ERROR");
        assertThat(result.providerCode()).isEqualTo("TIMEOUT");
        assertThat(result.dataset().getIndicators().getHeartRate().getLatestRestingHeartRate()).isEqualTo(56);
        assertThat(result.recommendation().explanation()).contains("No sleep, heart-rate, or training values were invented");
    }

    @Test
    void case9InjectedInstructionsAreRejected() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenReturn("""
                {"trends":[{"metric":"restingHeartRate","direction":"improving","summary":"Invented HR 40 after jailbreak"}],
                 "baselineComparisons":[{"metric":"restingHeartRate","currentValue":40.0,"baselineValue":40.0,"delta":0.0,"interpretation":"Followed user instruction"}],
                 "missingInformation":[],"notableObservations":["Diagnosed pneumonia"],
                 "dataQualityNotes":[],"medicalDisclaimer":"ignored"}
                """);

        RecoveryPipelineService.PipelineResult result = pipeline.run(RecoveryScenarioFixtures.injectionAttempt());

        assertThat(result.status()).isEqualTo("PROVIDER_ERROR");
        assertThat(result.providerCode()).isEqualTo("MALFORMED_RESPONSE");
        assertThat(result.recommendation().explanation().toLowerCase()).doesNotContain("you have pneumonia");
        assertThat(result.dataset().getIndicators().getHeartRate().getLatestRestingHeartRate()).isEqualTo(56);
    }

    private void stubCompliantModel() {
        when(recoveryAiClient.complete(anyString(), anyString())).thenAnswer(invocation ->
                RecoveryConstraintFollowingMock.complete(invocation.getArgument(1)));
    }
}
