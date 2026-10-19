package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fitcoach.app.backend.dto.RecoveryOverviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryPipelineServiceTest {

    @Mock
    private RecoveryAiClient recoveryAiClient;

    private RecoveryPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RecoveryPromptCatalog catalog = new RecoveryPromptCatalog();
        pipelineService = new RecoveryPipelineService(
                catalog,
                recoveryAiClient,
                new RecoveryResponseValidator(mapper),
                new RecoveryJson(mapper)
        );
    }

    @Test
    void skipsAiWhenDataIsInsufficient() {
        RecoveryDataset dataset = RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder().build())
                .minimumSignalPresent(false)
                .build();

        RecoveryPipelineService.PipelineResult result = pipelineService.run(dataset);

        assertThat(result.status()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(result.assessment().classification()).isEqualTo("INSUFFICIENT_INFORMATION");
        assertThat(result.recommendation().recommendedAction()).isEqualTo("LIGHT_RECOVERY");
        verify(recoveryAiClient, never()).complete(anyString(), anyString());
    }

    @Test
    void skipsAiAndForcesRestForConcerningSymptoms() {
        RecoveryDataset dataset = RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder()
                        .symptoms(RecoveryOverviewResponse.SymptomIndicator.builder()
                                .symptoms("fainting after intervals")
                                .available(true)
                                .build())
                        .build())
                .minimumSignalPresent(true)
                .concerningSymptomsPresent(true)
                .build();

        RecoveryPipelineService.PipelineResult result = pipelineService.run(dataset);

        assertThat(result.status()).isEqualTo("SAFETY_HOLD");
        assertThat(result.recommendation().recommendedAction()).isEqualTo("REST");
        assertThat(result.recommendation().requiresProfessionalEvaluation()).isTrue();
        verify(recoveryAiClient, never()).complete(anyString(), anyString());
    }

    @Test
    void passesValidatedStageOutputToTheNextPrompt() {
        RecoveryDataset dataset = RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder()
                        .sleep(RecoveryOverviewResponse.SleepIndicator.builder()
                                .latestHours(6.0)
                                .available(true)
                                .build())
                        .build())
                .minimumSignalPresent(true)
                .build();

        when(recoveryAiClient.complete(anyString(), anyString())).thenAnswer(invocation -> {
            String userMessage = invocation.getArgument(1);
            if (userMessage.contains("Validated recovery dataset JSON") && !userMessage.contains("Prompt A")) {
                return """
                        {"trends":[{"metric":"sleep","direction":"declining","summary":"Sleep is short"}],
                         "baselineComparisons":[],"missingInformation":["RHR"],"notableObservations":["Short sleep"],
                         "dataQualityNotes":[],"medicalDisclaimer":"Not a diagnosis"}
                        """;
            }
            if (userMessage.contains("Prompt A context-analysis JSON") && !userMessage.contains("Prompt B")) {
                assertThat(userMessage).contains("Short sleep");
                return """
                        {"classification":"MIXED","confidence":"MODERATE","supportingEvidence":["Short sleep"],
                         "uncertainties":["No RHR"],"safetyFlags":[]}
                        """;
            }
            assertThat(userMessage).contains("MIXED");
            return """
                    {"recommendedAction":"REDUCE_INTENSITY","nextSessionGuidance":"Keep sets easy",
                     "explanation":"Sleep is short","recoverySuggestions":["Prioritize bedtime"],
                     "safetyGuidance":"Stop if pain appears","requiresProfessionalEvaluation":false}
                    """;
        });

        RecoveryPipelineService.PipelineResult result = pipelineService.run(dataset);

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.assessment().classification()).isEqualTo("MIXED");
        assertThat(result.recommendation().recommendedAction()).isEqualTo("REDUCE_INTENSITY");
    }

    @Test
    void doesNotCallLaterStagesWhenPromptAIsMalformed() {
        RecoveryDataset dataset = RecoveryDataset.builder()
                .indicators(RecoveryOverviewResponse.Indicators.builder()
                        .sleep(RecoveryOverviewResponse.SleepIndicator.builder().latestHours(7.0).available(true).build())
                        .build())
                .minimumSignalPresent(true)
                .build();
        when(recoveryAiClient.complete(anyString(), anyString())).thenReturn("not-json");

        RecoveryPipelineService.PipelineResult result = pipelineService.run(dataset);

        assertThat(result.status()).isEqualTo("PROVIDER_ERROR");
        assertThat(result.providerCode()).isEqualTo("MALFORMED_RESPONSE");
        verify(recoveryAiClient).complete(anyString(), anyString());
    }
}
