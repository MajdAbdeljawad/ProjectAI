package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.ProgressAnalysisRequest;
import com.fitcoach.app.backend.dto.ProgressAnalysisRequest.MeasurementInput;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse.ProgressStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgressAnalysisServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);

    @Mock
    private AiCompletionClient aiClient;

    private ProgressAnalysisService service;

    @BeforeEach
    void setUp() {
        when(aiClient.getAiModel()).thenReturn("test-model");
        service = new ProgressAnalysisService(
                new ProgressCalculationsService(),
                new ProgressPromptService(),
                aiClient
        );
    }

    @Nested
    @DisplayName("Parsing de la réponse IA")
    class AiParsing {

        @Test
        @DisplayName("JSON valide → coachComment + exactement 3 tips")
        void parsesValidJsonEvenWithSurroundingText() throws Exception {
            String content = """
                Sure:
                {"coachComment":"Solid progress.","tips":["Tip A","Tip B","Tip C"]}
                """;

            ProgressAnalysisService.CoachAdvice advice = service.parse(content);

            assertThat(advice.coachComment()).isEqualTo("Solid progress.");
            assertThat(advice.tips()).containsExactly("Tip A", "Tip B", "Tip C");
        }

        @Test
        @DisplayName("JSON invalide (pas JSON) → message de secours")
        void fallsBackWhenAiJsonIsInvalid() {
            when(aiClient.completeJson(anyString(), anyString())).thenReturn("not json at all");

            ProgressAnalysisResponse result = service.analyze(progressingRequest());

            assertThat(result.getSource()).isEqualTo("fallback");
            assertThat(result.getCoachComment()).isNotBlank();
            assertThat(result.getTips()).hasSize(3);
            assertThat(result.getStats().getWeeklyTrendKg()).isEqualTo(-0.5);
            verify(aiClient).completeJson(eq(ProgressPromptService.SYSTEM_PROMPT), anyString());
        }

        @Test
        @DisplayName("JSON sans exactement 3 tips → rejeté puis fallback")
        void fallsBackWhenTipCountIsWrong() {
            when(aiClient.completeJson(anyString(), anyString())).thenReturn("""
                    {"coachComment":"Almost","tips":["one","two"]}
                    """);

            ProgressAnalysisResponse result = service.analyze(progressingRequest());

            assertThat(result.getSource()).isEqualTo("fallback");
            assertThat(result.getTips()).hasSize(3);
            assertThatThrownBy(() -> service.parse("""
                    {"coachComment":"x","tips":["one","two"]}
                    """)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly 3 tips");
        }

        @Test
        @DisplayName("IA indisponible → message de secours + prompt toujours exposé")
        void fallsBackWhenAiIsUnavailable() {
            when(aiClient.completeJson(anyString(), anyString()))
                    .thenThrow(new IllegalStateException("Ollama offline"));

            ProgressAnalysisResponse result = service.analyze(progressingRequest());

            assertThat(result.getSource()).isEqualTo("fallback");
            assertThat(result.getModel()).isEqualTo("test-model");
            assertThat(result.getPrompt()).contains("CALCULATED STATUS");
            assertThat(result.getPrompt()).contains("Status: PROGRESSING");
            assertThat(result.getCoachComment()).containsIgnoringCase("progress");
            assertThat(result.getTips()).hasSize(3);
            assertThat(result.getStatus()).isEqualTo(ProgressStatus.PROGRESSING);
            assertThat(result.getStats().getEstimatedGoalDate()).isNotNull();
            verify(aiClient).completeJson(eq(ProgressPromptService.SYSTEM_PROMPT), anyString());
        }

        @Test
        @DisplayName("JSON valide via le client mock → source=ai")
        void usesAiAdviceWhenProviderReturnsValidJson() {
            when(aiClient.completeJson(anyString(), anyString())).thenReturn("""
                    {"coachComment":"You are progressing well.","tips":["Sleep 7h","Lift twice","Walk daily"]}
                    """);

            ProgressAnalysisResponse result = service.analyze(progressingRequest());

            assertThat(result.getSource()).isEqualTo("ai");
            assertThat(result.getCoachComment()).isEqualTo("You are progressing well.");
            assertThat(result.getTips()).containsExactly("Sleep 7h", "Lift twice", "Walk daily");
            assertThat(result.getPrompt()).contains("Weekly trend:");
            verify(aiClient).completeJson(eq(ProgressPromptService.SYSTEM_PROMPT), anyString());
        }
    }

    private ProgressAnalysisRequest progressingRequest() {
        return request(75.0, m(0, 80.0), m(7, 79.5), m(14, 79.0), m(21, 78.5), m(28, 78.0));
    }

    private MeasurementInput m(int day, double weightKg) {
        MeasurementInput measurement = new MeasurementInput();
        measurement.setMeasuredOn(START.plusDays(day));
        measurement.setWeightKg(weightKg);
        return measurement;
    }

    private ProgressAnalysisRequest request(double targetWeightKg, MeasurementInput... measurements) {
        ProgressAnalysisRequest request = new ProgressAnalysisRequest();
        request.setFirstName("Oussama");
        request.setHeightCm(178.0);
        request.setMainGoal("fat loss");
        request.setTargetWeightKg(targetWeightKg);
        request.setMeasurements(new ArrayList<>(List.of(measurements)));
        return request;
    }
}
