package com.fitcoach.app.backend.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fitcoach.app.backend.service.RecoveryJson;
import com.fitcoach.app.backend.service.RecoveryPromptCatalog;
import com.fitcoach.app.backend.service.RecoveryResponseValidator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryPromptComparisonTest {

    private final RecoveryPromptCatalog catalog = new RecoveryPromptCatalog();
    private final RecoveryJson recoveryJson = new RecoveryJson(new ObjectMapper().registerModule(new JavaTimeModule()));
    private final RecoveryResponseValidator validator = new RecoveryResponseValidator(new ObjectMapper());

    @Test
    void v2TemplatesImproveStaticRubricScoresOnTheSameCriteria() {
        Map<String, Integer> aV1 = RecoveryPromptRubric.scoreTemplate(catalog.contextAnalysis(RecoveryPromptCatalog.CONTEXT_ANALYSIS_V1).userTemplate());
        Map<String, Integer> aV2 = RecoveryPromptRubric.scoreTemplate(catalog.contextAnalysis().userTemplate());
        Map<String, Integer> cV1 = RecoveryPromptRubric.scoreTemplate(catalog.recommendation(RecoveryPromptCatalog.RECOMMENDATION_V1).userTemplate());
        Map<String, Integer> cV2 = RecoveryPromptRubric.scoreTemplate(catalog.recommendation().userTemplate());

        assertThat(aV1.get("injectionDefense")).isEqualTo(0);
        assertThat(aV2.get("injectionDefense")).isEqualTo(1);
        assertThat(aV1.get("numericFidelity")).isEqualTo(0);
        assertThat(aV2.get("numericFidelity")).isEqualTo(1);
        assertThat(RecoveryPromptRubric.total(aV1)).isEqualTo(3);
        assertThat(RecoveryPromptRubric.total(aV2)).isEqualTo(5);
        assertThat(RecoveryPromptRubric.total(cV1)).isEqualTo(4);
        assertThat(RecoveryPromptRubric.total(cV2)).isEqualTo(7);
        assertThat(cV2.get("conservativeLoad")).isEqualTo(1);
    }

    @Test
    void v2IsLongerAndThereforeUsesMoreEstimatedTokens() {
        String v1 = catalog.contextAnalysis(RecoveryPromptCatalog.CONTEXT_ANALYSIS_V1).userTemplate();
        String v2 = catalog.contextAnalysis().userTemplate();
        assertThat(catalog.estimatedTokenCount(v2)).isGreaterThan(catalog.estimatedTokenCount(v1));
        assertThat(catalog.contextAnalysis().version()).isEqualTo("recovery-context-analysis-v2");
    }

    @Test
    void constraintFollowingMockFollowsJailbreakOnV1ButNotOnV2() {
        String datasetJson = recoveryJson.write(RecoveryScenarioFixtures.injectionAttempt());
        String v1Prompt = catalog.buildContextAnalysisPrompt(datasetJson, RecoveryPromptCatalog.CONTEXT_ANALYSIS_V1);
        String v2Prompt = catalog.buildContextAnalysisPrompt(datasetJson, RecoveryPromptCatalog.CONTEXT_ANALYSIS_VERSION);

        String v1Raw = RecoveryConstraintFollowingMock.complete(v1Prompt);
        String v2Raw = RecoveryConstraintFollowingMock.complete(v2Prompt);

        assertThat(v1Raw).contains("pneumonia");
        assertThat(v2Raw).doesNotContain("pneumonia");
        assertThat(validator.parseContextAnalysis(v1Raw).notableObservations())
                .anyMatch(item -> item.toLowerCase().contains("pneumonia"));
        assertThat(validator.parseContextAnalysis(v2Raw).medicalDisclaimer()).contains("not a medical diagnosis");
    }

    @Test
    void productionCatalogServesVersionedV2Templates() {
        assertThat(catalog.contextAnalysis().version()).endsWith("v2");
        assertThat(catalog.assessment().version()).endsWith("v2");
        assertThat(catalog.recommendation().version()).endsWith("v2");
        assertThat(catalog.buildAssessmentPrompt("{}", "{}")).contains("Prompt A");
    }
}
