package com.fitcoach.app.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_assessments")
@Getter
@Setter
@NoArgsConstructor
public class RecoveryAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private String status;

    private String classification;
    private String recommendedAction;
    private String confidence;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String nextSessionGuidance;

    @Column(columnDefinition = "TEXT")
    private String safetyGuidance;

    @Column(columnDefinition = "TEXT")
    private String safetyNotice;

    @Column(columnDefinition = "TEXT")
    private String recoverySuggestions;

    @Column(columnDefinition = "TEXT")
    private String missingInformation;

    @Column(columnDefinition = "TEXT")
    private String supportingEvidence;

    @Column(columnDefinition = "TEXT")
    private String uncertainties;

    @Column(columnDefinition = "TEXT")
    private String safetyFlags;

    @Column(columnDefinition = "TEXT")
    private String indicatorsJson;

    @Column(columnDefinition = "TEXT")
    private String contextAnalysisJson;

    @Column(columnDefinition = "TEXT")
    private String assessmentJson;

    @Column(columnDefinition = "TEXT")
    private String recommendationJson;

    @Column(name = "prompt_a_version")
    private String promptAVersion;

    @Column(name = "prompt_b_version")
    private String promptBVersion;

    @Column(name = "prompt_c_version")
    private String promptCVersion;

    @Column(name = "data_freshness_hours")
    private Integer dataFreshnessHours;

    @Column(nullable = false)
    private boolean requiresProfessionalEvaluation;

    @Column(nullable = false)
    private boolean planChangeAuthorized;

    @Column(columnDefinition = "TEXT")
    private String providerError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (generatedAt == null) {
            generatedAt = createdAt;
        }
    }
}
