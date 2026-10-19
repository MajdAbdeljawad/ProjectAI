package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryAssessmentResponse {
    private Long id;
    private LocalDateTime generatedAt;
    private String status;
    private String classification;
    private String recommendedAction;
    private String confidence;
    private String explanation;
    private String nextSessionGuidance;
    private String safetyGuidance;
    private String safetyNotice;
    @Builder.Default
    private List<String> recoverySuggestions = new ArrayList<>();
    @Builder.Default
    private List<String> missingInformation = new ArrayList<>();
    @Builder.Default
    private List<String> supportingEvidence = new ArrayList<>();
    @Builder.Default
    private List<String> uncertainties = new ArrayList<>();
    @Builder.Default
    private List<String> safetyFlags = new ArrayList<>();
    private RecoveryOverviewResponse.Indicators indicators;
    private String promptAVersion;
    private String promptBVersion;
    private String promptCVersion;
    private Integer dataFreshnessHours;
    private boolean requiresProfessionalEvaluation;
    private boolean planChangeAuthorized;
    private String providerError;
}
