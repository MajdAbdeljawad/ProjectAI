package com.fitcoach.app.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryPromptCatalogTest {

    private final RecoveryPromptCatalog catalog = new RecoveryPromptCatalog();

    @Test
    void exposesVersionedReusableTemplates() {
        assertThat(catalog.contextAnalysis().version()).isEqualTo("recovery-context-analysis-v2");
        assertThat(catalog.assessment().version()).isEqualTo("recovery-assessment-v2");
        assertThat(catalog.recommendation().version()).isEqualTo("recovery-recommendation-v2");
        assertThat(catalog.contextAnalysis(RecoveryPromptCatalog.CONTEXT_ANALYSIS_V1).version())
                .isEqualTo("recovery-context-analysis-v1");
        assertThat(catalog.contextAnalysis().userTemplate()).contains("not a medical diagnosis");
        assertThat(catalog.contextAnalysis().userTemplate()).contains("untrusted");
        assertThat(catalog.buildAssessmentPrompt("{\"sleep\":null}", "{\"trends\":[]}")).contains("Prompt A");
        assertThat(catalog.buildRecommendationPrompt("{}", "{}", "{}")).contains("Prompt B");
    }
}
