package com.fitcoach.app.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecoveryPipelineService {

    static final String DISCLAIMER =
            "This is not a medical diagnosis and does not replace professional evaluation. Do not train through significant pain or alarming symptoms.";

    private final RecoveryPromptCatalog promptCatalog;
    private final RecoveryAiClient recoveryAiClient;
    private final RecoveryResponseValidator responseValidator;
    private final RecoveryJson recoveryJson;

    public PipelineResult run(RecoveryDataset dataset) {
        RecoverySafetyPolicy.SafetyDecision safety = RecoverySafetyPolicy.evaluate(dataset);
        if (safety.blocksTraining()) {
            return deterministicSafety(dataset, safety);
        }
        if (!dataset.hasMinimumSignal()) {
            return deterministicInsufficient(dataset, safety);
        }

        String datasetJson = recoveryJson.write(datasetPayload(dataset));
        try {
            RecoveryPromptCatalog.PromptTemplate promptA = promptCatalog.contextAnalysis();
            String rawA = recoveryAiClient.complete(promptA.systemMessage(), promptCatalog.buildContextAnalysisPrompt(datasetJson));
            RecoveryPipelineModels.ContextAnalysis context = responseValidator.parseContextAnalysis(rawA);
            RecoveryOutputGuard.assertNoDisallowedClaims(context);
            String contextJson = recoveryJson.write(context);

            RecoveryPromptCatalog.PromptTemplate promptB = promptCatalog.assessment();
            String rawB = recoveryAiClient.complete(promptB.systemMessage(), promptCatalog.buildAssessmentPrompt(datasetJson, contextJson));
            RecoveryPipelineModels.Assessment assessment = responseValidator.parseAssessment(rawB);
            assessment = applySafetyToAssessment(assessment, safety, dataset);
            RecoveryOutputGuard.assertNoDisallowedClaims(assessment);
            String assessmentJson = recoveryJson.write(assessment);

            RecoveryPromptCatalog.PromptTemplate promptC = promptCatalog.recommendation();
            String rawC = recoveryAiClient.complete(promptC.systemMessage(), promptCatalog.buildRecommendationPrompt(datasetJson, contextJson, assessmentJson));
            RecoveryPipelineModels.Recommendation recommendation = responseValidator.parseRecommendation(rawC);
            recommendation = applySafetyToRecommendation(recommendation, safety, assessment);
            RecoveryOutputGuard.assertNoDisallowedClaims(context, assessment, recommendation);

            return PipelineResult.success(dataset, safety, context, assessment, recommendation);
        } catch (RecoveryProviderException e) {
            return PipelineResult.providerError(dataset, safety, e);
        }
    }

    private RecoveryPipelineModels.Assessment applySafetyToAssessment(RecoveryPipelineModels.Assessment assessment,
                                                                      RecoverySafetyPolicy.SafetyDecision safety,
                                                                      RecoveryDataset dataset) {
        List<String> flags = new ArrayList<>(assessment.safetyFlags());
        flags.addAll(safety.flags());
        flags.addAll(dataset.getSafetyFlags());
        String classification = assessment.classification();
        if (safety.blocksTraining() || safety.significantPain()) {
            classification = "ADDITIONAL_RECOVERY_RECOMMENDED";
        }
        return new RecoveryPipelineModels.Assessment(
                classification,
                assessment.confidence(),
                assessment.supportingEvidence(),
                assessment.uncertainties(),
                flags.stream().distinct().toList()
        );
    }

    private RecoveryPipelineModels.Recommendation applySafetyToRecommendation(RecoveryPipelineModels.Recommendation recommendation,
                                                                              RecoverySafetyPolicy.SafetyDecision safety,
                                                                              RecoveryPipelineModels.Assessment assessment) {
        String action = RecoverySafetyPolicy.overrideAction(recommendation.recommendedAction(), safety);
        if ("INSUFFICIENT_INFORMATION".equals(assessment.classification()) && "CONTINUE_PLAN".equals(action)) {
            action = "LIGHT_RECOVERY";
        }
        boolean professional = Boolean.TRUE.equals(recommendation.requiresProfessionalEvaluation())
                || safety.requiresProfessionalEvaluation();
        String safetyGuidance = recommendation.safetyGuidance();
        if (safety.safetyNotice() != null) {
            safetyGuidance = safety.safetyNotice();
        }
        return new RecoveryPipelineModels.Recommendation(
                action,
                recommendation.nextSessionGuidance(),
                recommendation.explanation(),
                recommendation.recoverySuggestions(),
                safetyGuidance,
                professional
        );
    }

    private PipelineResult deterministicSafety(RecoveryDataset dataset, RecoverySafetyPolicy.SafetyDecision safety) {
        RecoveryPipelineModels.ContextAnalysis context = new RecoveryPipelineModels.ContextAnalysis(
                List.of(),
                List.of(),
                dataset.getMissingFields(),
                List.of("Deterministic safety review replaced the AI pipeline because warning signs were present."),
                dataset.getDataQualityNotes(),
                DISCLAIMER
        );
        RecoveryPipelineModels.Assessment assessment = new RecoveryPipelineModels.Assessment(
                "ADDITIONAL_RECOVERY_RECOMMENDED",
                "HIGH",
                List.of("Reported symptoms or pain crossed a safety threshold handled by application rules."),
                dataset.getMissingFields(),
                safety.flags()
        );
        RecoveryPipelineModels.Recommendation recommendation = new RecoveryPipelineModels.Recommendation(
                safety.forcedAction(),
                "Do not complete the planned session. Rest and seek appropriate professional evaluation.",
                safety.explanation(),
                List.of("Stop training", "Note symptoms for a clinician or qualified professional", "Do not use this app as a diagnosis"),
                safety.safetyNotice(),
                true
        );
        return PipelineResult.safety(dataset, safety, context, assessment, recommendation);
    }

    private PipelineResult deterministicInsufficient(RecoveryDataset dataset, RecoverySafetyPolicy.SafetyDecision safety) {
        RecoveryPipelineModels.ContextAnalysis context = new RecoveryPipelineModels.ContextAnalysis(
                List.of(),
                List.of(),
                dataset.getMissingFields(),
                List.of("There is not enough available recovery data for a model-based assessment."),
                dataset.getDataQualityNotes(),
                DISCLAIMER
        );
        RecoveryPipelineModels.Assessment assessment = new RecoveryPipelineModels.Assessment(
                "INSUFFICIENT_INFORMATION",
                "LOW",
                List.of("Available indicators are missing or too sparse to classify recovery readiness."),
                dataset.getMissingFields(),
                safety.flags()
        );
        RecoveryPipelineModels.Recommendation recommendation = new RecoveryPipelineModels.Recommendation(
                "LIGHT_RECOVERY",
                "Keep load conservative and log sleep, fatigue, soreness, and resting heart rate before hard sessions.",
                "Too little recovery data is available to justify continuing or increasing planned training. Missing values were left unknown rather than estimated.",
                List.of("Log tonight's sleep duration", "Rate fatigue and soreness after waking", "Add a resting heart-rate reading if you already track one"),
                DISCLAIMER,
                false
        );
        return PipelineResult.insufficient(dataset, safety, context, assessment, recommendation);
    }

    private Map<String, Object> datasetPayload(RecoveryDataset dataset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("indicators", dataset.getIndicators());
        payload.put("missingFields", dataset.getMissingFields());
        payload.put("dataQualityNotes", dataset.getDataQualityNotes());
        payload.put("contradictions", dataset.getContradictions());
        payload.put("safetyFlags", dataset.getSafetyFlags());
        payload.put("oldestInputAgeHours", dataset.getOldestInputAgeHours());
        payload.put("minimumSignalPresent", dataset.isMinimumSignalPresent());
        payload.put("instruction", "Null means unknown. Do not invent values.");
        return payload;
    }

    public record PipelineResult(
            String status,
            RecoveryDataset dataset,
            RecoverySafetyPolicy.SafetyDecision safety,
            RecoveryPipelineModels.ContextAnalysis contextAnalysis,
            RecoveryPipelineModels.Assessment assessment,
            RecoveryPipelineModels.Recommendation recommendation,
            String providerError,
            String providerCode
    ) {
        static PipelineResult success(RecoveryDataset dataset,
                                      RecoverySafetyPolicy.SafetyDecision safety,
                                      RecoveryPipelineModels.ContextAnalysis context,
                                      RecoveryPipelineModels.Assessment assessment,
                                      RecoveryPipelineModels.Recommendation recommendation) {
            return new PipelineResult("SUCCESS", dataset, safety, context, assessment, recommendation, null, null);
        }

        static PipelineResult safety(RecoveryDataset dataset,
                                     RecoverySafetyPolicy.SafetyDecision safety,
                                     RecoveryPipelineModels.ContextAnalysis context,
                                     RecoveryPipelineModels.Assessment assessment,
                                     RecoveryPipelineModels.Recommendation recommendation) {
            return new PipelineResult("SAFETY_HOLD", dataset, safety, context, assessment, recommendation, null, null);
        }

        static PipelineResult insufficient(RecoveryDataset dataset,
                                           RecoverySafetyPolicy.SafetyDecision safety,
                                           RecoveryPipelineModels.ContextAnalysis context,
                                           RecoveryPipelineModels.Assessment assessment,
                                           RecoveryPipelineModels.Recommendation recommendation) {
            return new PipelineResult("INSUFFICIENT_DATA", dataset, safety, context, assessment, recommendation, null, null);
        }

        static PipelineResult providerError(RecoveryDataset dataset,
                                            RecoverySafetyPolicy.SafetyDecision safety,
                                            RecoveryProviderException error) {
            RecoveryPipelineModels.ContextAnalysis context = new RecoveryPipelineModels.ContextAnalysis(
                    List.of(),
                    List.of(),
                    dataset.getMissingFields(),
                    List.of("The recovery model was unavailable or returned an invalid response."),
                    dataset.getDataQualityNotes(),
                    DISCLAIMER
            );
            RecoveryPipelineModels.Assessment assessment = new RecoveryPipelineModels.Assessment(
                    "INSUFFICIENT_INFORMATION",
                    "LOW",
                    List.of("No model classification was accepted."),
                    List.of("Provider error: " + error.getCode()),
                    safety.flags()
            );
            RecoveryPipelineModels.Recommendation recommendation = new RecoveryPipelineModels.Recommendation(
                    "LIGHT_RECOVERY",
                    "The recovery model could not complete an assessment. Do not increase training load.",
                    "Available measurements were kept, but no AI recommendation was accepted because the provider failed schema validation, timed out, or was unavailable. No sleep, heart-rate, or training values were invented.",
                    List.of("Retry once the model is available", "Keep today's session easy or skip it if you feel unwell"),
                    DISCLAIMER,
                    safety.requiresProfessionalEvaluation()
            );
            return new PipelineResult("PROVIDER_ERROR", dataset, safety, context, assessment, recommendation, error.getMessage(), error.getCode());
        }
    }
}
