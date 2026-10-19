package com.fitcoach.app.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RecoveryResponseValidator {

    private static final Set<String> DIRECTIONS = Set.of("improving", "declining", "stable", "unknown");
    private static final Set<String> CLASSIFICATIONS = Set.of(
            "FAVORABLE", "MIXED", "ADDITIONAL_RECOVERY_RECOMMENDED", "INSUFFICIENT_INFORMATION"
    );
    private static final Set<String> CONFIDENCE = Set.of("LOW", "MODERATE", "HIGH");
    private static final Set<String> ACTIONS = Set.of(
            "CONTINUE_PLAN", "REDUCE_INTENSITY", "REDUCE_VOLUME", "LIGHT_RECOVERY", "REST"
    );

    private static final String DEFAULT_DISCLAIMER =
            "This analysis is not a medical diagnosis and does not replace professional evaluation.";

    private final ObjectMapper objectMapper;

    public RecoveryResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RecoveryPipelineModels.ContextAnalysis parseContextAnalysis(String raw) {
        RecoveryPipelineModels.ContextAnalysis parsed = read(raw, RecoveryPipelineModels.ContextAnalysis.class, "Prompt A");
        List<RecoveryPipelineModels.Trend> trends = parsed.trends().stream()
                .map(trend -> new RecoveryPipelineModels.Trend(
                        required(trend.metric(), "Prompt A trend metric"),
                        normalizeDirection(trend.direction()),
                        required(trend.summary(), "Prompt A trend summary")
                ))
                .toList();
        String disclaimer = parsed.medicalDisclaimer() == null || parsed.medicalDisclaimer().isBlank()
                ? DEFAULT_DISCLAIMER : parsed.medicalDisclaimer();
        return new RecoveryPipelineModels.ContextAnalysis(
                trends,
                parsed.baselineComparisons(),
                parsed.missingInformation(),
                parsed.notableObservations(),
                parsed.dataQualityNotes(),
                disclaimer
        );
    }

    public RecoveryPipelineModels.Assessment parseAssessment(String raw) {
        RecoveryPipelineModels.Assessment parsed = read(raw, RecoveryPipelineModels.Assessment.class, "Prompt B");
        String classification = normalizeEnum(parsed.classification(), CLASSIFICATIONS, "Prompt B classification");
        String confidence = normalizeEnum(parsed.confidence() == null ? "LOW" : parsed.confidence(), CONFIDENCE, "Prompt B confidence");
        if (parsed.supportingEvidence().isEmpty() && parsed.uncertainties().isEmpty()) {
            throw new RecoveryProviderException("Prompt B did not include evidence or uncertainty.", "MALFORMED_RESPONSE");
        }
        return new RecoveryPipelineModels.Assessment(
                classification,
                confidence,
                parsed.supportingEvidence(),
                parsed.uncertainties(),
                parsed.safetyFlags()
        );
    }

    public RecoveryPipelineModels.Recommendation parseRecommendation(String raw) {
        RecoveryPipelineModels.Recommendation parsed = read(raw, RecoveryPipelineModels.Recommendation.class, "Prompt C");
        String action = normalizeEnum(parsed.recommendedAction(), ACTIONS, "Prompt C recommendedAction");
        return new RecoveryPipelineModels.Recommendation(
                action,
                required(parsed.nextSessionGuidance(), "Prompt C nextSessionGuidance"),
                required(parsed.explanation(), "Prompt C explanation"),
                parsed.recoverySuggestions(),
                required(parsed.safetyGuidance(), "Prompt C safetyGuidance"),
                parsed.requiresProfessionalEvaluation() != null && parsed.requiresProfessionalEvaluation()
        );
    }

    private <T> T read(String raw, Class<T> type, String stage) {
        try {
            String json = extractJsonObject(raw);
            T value = objectMapper.readValue(json, type);
            if (value == null) {
                throw new RecoveryProviderException(stage + " returned an empty object.", "MALFORMED_RESPONSE");
            }
            return value;
        } catch (RecoveryProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new RecoveryProviderException(stage + " returned JSON that did not match the expected schema.", "MALFORMED_RESPONSE", e);
        }
    }

    String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RecoveryProviderException("The recovery model returned an empty body.", "MALFORMED_RESPONSE");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new RecoveryProviderException("The recovery model did not return a JSON object.", "MALFORMED_RESPONSE");
        }
        return trimmed.substring(start, end + 1);
    }

    private String normalizeDirection(String raw) {
        String value = raw == null ? "unknown" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "improving", "better", "up" -> "improving";
            case "declining", "worse", "down" -> "declining";
            case "stable", "unchanged", "flat" -> "stable";
            case "unknown", "unclear", "n/a", "na" -> "unknown";
            default -> {
                if (DIRECTIONS.contains(value)) {
                    yield value;
                }
                throw new RecoveryProviderException("Prompt A used an unsupported trend direction.", "MALFORMED_RESPONSE");
            }
        };
    }

    private String normalizeEnum(String raw, Set<String> allowed, String field) {
        if (raw == null || raw.isBlank()) {
            throw new RecoveryProviderException(field + " was missing.", "MALFORMED_RESPONSE");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!allowed.contains(normalized)) {
            throw new RecoveryProviderException(field + " had an unsupported value.", "MALFORMED_RESPONSE");
        }
        return normalized;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RecoveryProviderException(field + " was missing.", "MALFORMED_RESPONSE");
        }
        return value.trim();
    }
}
