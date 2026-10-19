package com.fitcoach.app.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecoveryCheckinRequest {
    private LocalDate recordedOn;

    @DecimalMin(value = "0.0", message = "Sleep duration cannot be negative")
    @DecimalMax(value = "960.0", message = "Sleep duration is outside an acceptable range")
    private Double sleepHours;

    @Min(value = 1, message = "Sleep quality must be between 1 and 5")
    @Max(value = 5, message = "Sleep quality must be between 1 and 5")
    private Integer sleepQuality;

    @Min(value = 1, message = "Fatigue must be between 1 and 10")
    @Max(value = 10, message = "Fatigue must be between 1 and 10")
    private Integer fatigueLevel;

    @Min(value = 1, message = "Soreness must be between 1 and 10")
    @Max(value = 10, message = "Soreness must be between 1 and 10")
    private Integer sorenessLevel;

    private String sorenessAreas;

    @Min(value = 20, message = "Resting heart rate is outside an acceptable range")
    @Max(value = 220, message = "Resting heart rate is outside an acceptable range")
    private Integer restingHeartRate;

    private String symptoms;
    private String limitations;
    private String notes;
}
