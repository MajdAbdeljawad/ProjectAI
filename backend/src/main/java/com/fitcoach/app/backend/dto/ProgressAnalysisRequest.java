package com.fitcoach.app.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProgressAnalysisRequest {
    @NotBlank
    private String firstName;

    @NotNull
    @DecimalMin("100")
    @DecimalMax("250")
    private Double heightCm;

    @NotBlank
    private String mainGoal;

    @NotNull
    @DecimalMin("30")
    @DecimalMax("300")
    private Double targetWeightKg;

    @Future
    private LocalDate targetDate;

    @NotNull
    @Size(min = 1, max = 100)
    private List<@Valid @NotNull MeasurementInput> measurements;

    @Data
    public static class MeasurementInput {
        @NotNull
        @PastOrPresent
        private LocalDate measuredOn;

        @NotNull
        @DecimalMin("30")
        @DecimalMax("300")
        private Double weightKg;

        @DecimalMin("40")
        @DecimalMax("200")
        private Double waistCm;

        @DecimalMin("3")
        @DecimalMax("60")
        private Double bodyFatPercent;

        @Size(max = 500)
        private String notes;
    }
}
