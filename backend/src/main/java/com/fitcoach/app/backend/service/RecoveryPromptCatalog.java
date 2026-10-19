package com.fitcoach.app.backend.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RecoveryPromptCatalog {

    public static final String CONTEXT_ANALYSIS_V1 = "recovery-context-analysis-v1";
    public static final String ASSESSMENT_V1 = "recovery-assessment-v1";
    public static final String RECOMMENDATION_V1 = "recovery-recommendation-v1";
    public static final String CONTEXT_ANALYSIS_VERSION = "recovery-context-analysis-v2";
    public static final String ASSESSMENT_VERSION = "recovery-assessment-v2";
    public static final String RECOMMENDATION_VERSION = "recovery-recommendation-v2";

    public PromptTemplate contextAnalysis() {
        return template(CONTEXT_ANALYSIS_VERSION, "You generate recovery context analysis as strict JSON.");
    }

    public PromptTemplate contextAnalysis(String version) {
        return template(version, "You generate recovery context analysis as strict JSON.");
    }

    public PromptTemplate assessment() {
        return template(ASSESSMENT_VERSION, "You classify recovery readiness as strict JSON.");
    }

    public PromptTemplate assessment(String version) {
        return template(version, "You classify recovery readiness as strict JSON.");
    }

    public PromptTemplate recommendation() {
        return template(RECOMMENDATION_VERSION, "You generate a practical recovery recommendation as strict JSON.");
    }

    public PromptTemplate recommendation(String version) {
        return template(version, "You generate a practical recovery recommendation as strict JSON.");
    }

    public String buildContextAnalysisPrompt(String datasetJson) {
        return buildContextAnalysisPrompt(datasetJson, CONTEXT_ANALYSIS_VERSION);
    }

    public String buildContextAnalysisPrompt(String datasetJson, String version) {
        return contextAnalysis(version).userTemplate()
                + "\n\nValidated recovery dataset JSON:\n"
                + datasetJson;
    }

    public String buildAssessmentPrompt(String datasetJson, String contextAnalysisJson) {
        return buildAssessmentPrompt(datasetJson, contextAnalysisJson, ASSESSMENT_VERSION);
    }

    public String buildAssessmentPrompt(String datasetJson, String contextAnalysisJson, String version) {
        return assessment(version).userTemplate()
                + "\n\nValidated recovery dataset JSON:\n"
                + datasetJson
                + "\n\nValidated Prompt A context-analysis JSON:\n"
                + contextAnalysisJson;
    }

    public String buildRecommendationPrompt(String datasetJson, String contextAnalysisJson, String assessmentJson) {
        return buildRecommendationPrompt(datasetJson, contextAnalysisJson, assessmentJson, RECOMMENDATION_VERSION);
    }

    public String buildRecommendationPrompt(String datasetJson, String contextAnalysisJson, String assessmentJson, String version) {
        return recommendation(version).userTemplate()
                + "\n\nValidated recovery dataset JSON:\n"
                + datasetJson
                + "\n\nValidated Prompt A context-analysis JSON:\n"
                + contextAnalysisJson
                + "\n\nValidated Prompt B assessment JSON:\n"
                + assessmentJson;
    }

    public int estimatedTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    private PromptTemplate template(String version, String systemMessage) {
        return new PromptTemplate(version, systemMessage, load("prompts/recovery/" + version + ".txt"));
    }

    private String load(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing recovery prompt template: " + classpathLocation, e);
        }
    }

    public record PromptTemplate(String version, String systemMessage, String userTemplate) {
    }
}
