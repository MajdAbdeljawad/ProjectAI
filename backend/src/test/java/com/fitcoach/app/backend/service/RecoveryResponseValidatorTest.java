package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryResponseValidatorTest {

    private RecoveryResponseValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RecoveryResponseValidator(new ObjectMapper());
    }

    @Test
    void extractsJsonFromMarkdownAndValidatesPromptA() {
        String raw = """
                ```json
                {"trends":[{"metric":"sleep","direction":"stable","summary":"Sleep hours unchanged"}],
                 "baselineComparisons":[],
                 "missingInformation":["resting heart rate"],
                 "notableObservations":["No RHR baseline"],
                 "dataQualityNotes":[],
                 "medicalDisclaimer":"Not a diagnosis"}
                ```
                """;

        RecoveryPipelineModels.ContextAnalysis parsed = validator.parseContextAnalysis(raw);
        assertThat(parsed.trends()).hasSize(1);
        assertThat(parsed.missingInformation()).contains("resting heart rate");
    }

    @Test
    void rejectsMalformedPromptB() {
        assertThatThrownBy(() -> validator.parseAssessment("{\"classification\":\"unknown\"}"))
                .isInstanceOf(RecoveryProviderException.class)
                .extracting(ex -> ((RecoveryProviderException) ex).getCode())
                .isEqualTo("MALFORMED_RESPONSE");
    }

    @Test
    void validatesPromptCEnums() {
        String raw = """
                {"recommendedAction":"reduce intensity","nextSessionGuidance":"Keep the session easier",
                 "explanation":"Sleep was short","recoverySuggestions":["Go to bed earlier"],
                 "safetyGuidance":"Stop if pain increases","requiresProfessionalEvaluation":false}
                """;
        RecoveryPipelineModels.Recommendation parsed = validator.parseRecommendation(raw);
        assertThat(parsed.recommendedAction()).isEqualTo("REDUCE_INTENSITY");
    }
}
