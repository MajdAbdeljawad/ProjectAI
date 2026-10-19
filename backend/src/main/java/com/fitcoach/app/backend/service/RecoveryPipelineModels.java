package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

public final class RecoveryPipelineModels {

    private RecoveryPipelineModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextAnalysis(
            List<Trend> trends,
            List<BaselineComparison> baselineComparisons,
            List<String> missingInformation,
            List<String> notableObservations,
            List<String> dataQualityNotes,
            String medicalDisclaimer
    ) {
        public ContextAnalysis {
            trends = trends == null ? new ArrayList<>() : trends;
            baselineComparisons = baselineComparisons == null ? new ArrayList<>() : baselineComparisons;
            missingInformation = missingInformation == null ? new ArrayList<>() : missingInformation;
            notableObservations = notableObservations == null ? new ArrayList<>() : notableObservations;
            dataQualityNotes = dataQualityNotes == null ? new ArrayList<>() : dataQualityNotes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Trend(String metric, String direction, String summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BaselineComparison(
            String metric,
            Double currentValue,
            Double baselineValue,
            Double delta,
            String interpretation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Assessment(
            String classification,
            String confidence,
            List<String> supportingEvidence,
            List<String> uncertainties,
            List<String> safetyFlags
    ) {
        public Assessment {
            supportingEvidence = supportingEvidence == null ? new ArrayList<>() : supportingEvidence;
            uncertainties = uncertainties == null ? new ArrayList<>() : uncertainties;
            safetyFlags = safetyFlags == null ? new ArrayList<>() : safetyFlags;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recommendation(
            String recommendedAction,
            String nextSessionGuidance,
            String explanation,
            List<String> recoverySuggestions,
            String safetyGuidance,
            Boolean requiresProfessionalEvaluation
    ) {
        public Recommendation {
            recoverySuggestions = recoverySuggestions == null ? new ArrayList<>() : recoverySuggestions;
        }
    }
}
