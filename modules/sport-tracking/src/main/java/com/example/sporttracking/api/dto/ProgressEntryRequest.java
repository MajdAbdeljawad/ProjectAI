package com.example.sporttracking.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressEntryRequest(
        @NotNull LocalDate measurementDate,
        @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1000.0")
        @Digits(integer = 4, fraction = 2, message = "Le poids accepte au maximum 2 décimales") BigDecimal weight,
        @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1000.0")
        @Digits(integer = 4, fraction = 2, message = "Le tour de taille accepte au maximum 2 décimales") BigDecimal waistCircumference,
        @DecimalMin("0.0") @DecimalMax("24.0")
        @Digits(integer = 2, fraction = 2, message = "Le sommeil accepte au maximum 2 décimales") BigDecimal sleepHours,
        @Min(1) @Max(5) Integer energyLevel
) {
}
