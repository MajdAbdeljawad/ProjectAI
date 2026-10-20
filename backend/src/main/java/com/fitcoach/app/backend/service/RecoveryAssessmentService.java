package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.GenerateRecoveryRequest;
import com.fitcoach.app.backend.dto.RecoveryAssessmentResponse;
import com.fitcoach.app.backend.dto.RecoveryCheckinResponse;
import com.fitcoach.app.backend.dto.RecoveryOverviewResponse;
import com.fitcoach.app.backend.entity.RecoveryAssessment;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.exception.ApiException;
import com.fitcoach.app.backend.repository.RecoveryAssessmentRepository;
import com.fitcoach.app.backend.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecoveryAssessmentService {

    private final UserService userService;
    private final RecoveryDataPreparationService dataPreparationService;
    private final RecoveryCheckinService recoveryCheckinService;
    private final RecoveryPipelineService recoveryPipelineService;
    private final RecoveryAssessmentRepository recoveryAssessmentRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final RecoveryJson recoveryJson;

    @Transactional(readOnly = true)
    public RecoveryOverviewResponse overview() {
        User user = userService.getCurrentUser();
        RecoveryDataset dataset = dataPreparationService.prepare(user);
        RecoveryAssessment latest = recoveryAssessmentRepository.findFirstByUserIdOrderByGeneratedAtDesc(user.getId())
                .orElse(null);
        return toOverview(dataset, latest, recoveryCheckinService.listCurrentUser());
    }

    @Transactional
    public RecoveryAssessmentResponse generate(GenerateRecoveryRequest request) {
        User user = userService.getCurrentUser();
        if (request != null && request.getCheckin() != null) {
            recoveryCheckinService.saveForUser(user, request.getCheckin());
        }

        long planCountBefore = workoutPlanRepository.findByUserIdOrderByGeneratedAtDesc(user.getId()).size();
        RecoveryDataset dataset = dataPreparationService.prepare(user);
        RecoveryPipelineService.PipelineResult result = recoveryPipelineService.run(dataset);
        RecoveryAssessment saved = persist(user, result);
        assertPlanUnchanged(user, planCountBefore);
        return toResponse(saved, result.dataset().getIndicators());
    }

    @Transactional(readOnly = true)
    public List<RecoveryAssessmentResponse> history() {
        User user = userService.getCurrentUser();
        return recoveryAssessmentRepository.findByUserIdOrderByGeneratedAtDesc(user.getId())
                .stream()
                .map(assessment -> toResponse(assessment, recoveryJson.read(assessment.getIndicatorsJson(), RecoveryOverviewResponse.Indicators.class)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RecoveryAssessmentResponse getOwned(Long id) {
        User user = userService.getCurrentUser();
        RecoveryAssessment assessment = recoveryAssessmentRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException("Recovery assessment not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        return toResponse(assessment, recoveryJson.read(assessment.getIndicatorsJson(), RecoveryOverviewResponse.Indicators.class));
    }

    private RecoveryAssessment persist(User user, RecoveryPipelineService.PipelineResult result) {
        RecoveryAssessment assessment = new RecoveryAssessment();
        assessment.setUser(user);
        assessment.setGeneratedAt(LocalDateTime.now());
        assessment.setStatus(result.status());
        assessment.setClassification(result.assessment().classification());
        assessment.setRecommendedAction(result.recommendation().recommendedAction());
        assessment.setConfidence(result.assessment().confidence());
        assessment.setExplanation(result.recommendation().explanation());
        assessment.setNextSessionGuidance(result.recommendation().nextSessionGuidance());
        assessment.setSafetyGuidance(result.recommendation().safetyGuidance());
        assessment.setSafetyNotice(result.safety().safetyNotice() == null
                ? RecoveryPipelineService.DISCLAIMER
                : result.safety().safetyNotice());
        assessment.setRecoverySuggestions(recoveryJson.write(result.recommendation().recoverySuggestions()));
        assessment.setMissingInformation(recoveryJson.write(result.dataset().getMissingFields()));
        assessment.setSupportingEvidence(recoveryJson.write(result.assessment().supportingEvidence()));
        assessment.setUncertainties(recoveryJson.write(result.assessment().uncertainties()));
        assessment.setSafetyFlags(recoveryJson.write(result.assessment().safetyFlags()));
        assessment.setIndicatorsJson(recoveryJson.write(result.dataset().getIndicators()));
        assessment.setContextAnalysisJson(recoveryJson.write(result.contextAnalysis()));
        assessment.setAssessmentJson(recoveryJson.write(result.assessment()));
        assessment.setRecommendationJson(recoveryJson.write(result.recommendation()));
        assessment.setPromptAVersion(RecoveryPromptCatalog.CONTEXT_ANALYSIS_VERSION);
        assessment.setPromptBVersion(RecoveryPromptCatalog.ASSESSMENT_VERSION);
        assessment.setPromptCVersion(RecoveryPromptCatalog.RECOMMENDATION_VERSION);
        assessment.setDataFreshnessHours(result.dataset().getOldestInputAgeHours());
        assessment.setRequiresProfessionalEvaluation(Boolean.TRUE.equals(result.recommendation().requiresProfessionalEvaluation()));
        assessment.setPlanChangeAuthorized(false);
        assessment.setProviderError(result.providerError());
        return recoveryAssessmentRepository.save(assessment);
    }

    private void assertPlanUnchanged(User user, long planCountBefore) {
        long planCountAfter = workoutPlanRepository.findByUserIdOrderByGeneratedAtDesc(user.getId()).size();
        if (planCountAfter != planCountBefore) {
            throw new ApiException("Recovery assessment cannot change a training plan", HttpStatus.CONFLICT, "PLAN_MUTATION_FORBIDDEN");
        }
    }

    private RecoveryOverviewResponse toOverview(RecoveryDataset dataset,
                                                RecoveryAssessment latest,
                                                List<RecoveryCheckinResponse> checkins) {
        return RecoveryOverviewResponse.builder()
                .latestAssessment(latest == null ? null : toResponse(latest, dataset.getIndicators()))
                .indicators(dataset.getIndicators())
                .missingFields(dataset.getMissingFields())
                .dataQualityNotes(dataset.getDataQualityNotes())
                .contradictions(dataset.getContradictions())
                .safetyFlags(dataset.getSafetyFlags())
                .recentCheckins(checkins.stream().limit(14).toList())
                .minimumSignalPresent(dataset.isMinimumSignalPresent())
                .safetyNotice(RecoveryPipelineService.DISCLAIMER)
                .build();
    }

    RecoveryAssessmentResponse toResponse(RecoveryAssessment assessment, RecoveryOverviewResponse.Indicators indicators) {
        return RecoveryAssessmentResponse.builder()
                .id(assessment.getId())
                .generatedAt(assessment.getGeneratedAt())
                .status(assessment.getStatus())
                .classification(assessment.getClassification())
                .recommendedAction(assessment.getRecommendedAction())
                .confidence(assessment.getConfidence())
                .explanation(assessment.getExplanation())
                .nextSessionGuidance(assessment.getNextSessionGuidance())
                .safetyGuidance(assessment.getSafetyGuidance())
                .safetyNotice(assessment.getSafetyNotice())
                .recoverySuggestions(recoveryJson.readStringList(assessment.getRecoverySuggestions()))
                .missingInformation(recoveryJson.readStringList(assessment.getMissingInformation()))
                .supportingEvidence(recoveryJson.readStringList(assessment.getSupportingEvidence()))
                .uncertainties(recoveryJson.readStringList(assessment.getUncertainties()))
                .safetyFlags(recoveryJson.readStringList(assessment.getSafetyFlags()))
                .indicators(indicators)
                .promptAVersion(assessment.getPromptAVersion())
                .promptBVersion(assessment.getPromptBVersion())
                .promptCVersion(assessment.getPromptCVersion())
                .dataFreshnessHours(assessment.getDataFreshnessHours())
                .requiresProfessionalEvaluation(assessment.isRequiresProfessionalEvaluation())
                .planChangeAuthorized(false)
                .providerError(assessment.getProviderError())
                .build();
    }
}
