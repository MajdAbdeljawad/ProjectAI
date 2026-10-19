package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecoveryOverviewResponse;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class RecoveryDataset {
    private RecoveryOverviewResponse.Indicators indicators;
    @Builder.Default
    private List<String> missingFields = new ArrayList<>();
    @Builder.Default
    private List<String> dataQualityNotes = new ArrayList<>();
    @Builder.Default
    private List<String> contradictions = new ArrayList<>();
    @Builder.Default
    private List<String> safetyFlags = new ArrayList<>();
    private Integer oldestInputAgeHours;
    private boolean minimumSignalPresent;
    private boolean concerningSymptomsPresent;
    private boolean significantPainPresent;

    public boolean hasMinimumSignal() {
        return minimumSignalPresent;
    }
}
