package com.example.sporttracking.api.dto;

import com.example.sporttracking.progress.ProgressEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressEntryResponse(
        Long id,
        Long userId,
        LocalDate measurementDate,
        BigDecimal weight,
        BigDecimal waistCircumference,
        BigDecimal sleepHours,
        Integer energyLevel
) {
    public static ProgressEntryResponse from(ProgressEntry entry) {
        return new ProgressEntryResponse(entry.getId(), entry.getUserId(), entry.getMeasurementDate(),
                entry.getWeight(), entry.getWaistCircumference(), entry.getSleepHours(), entry.getEnergyLevel());
    }
}
