package com.example.sporttracking.api.dto;

import com.example.sporttracking.goal.GoalStatus;
import com.example.sporttracking.goal.GoalType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotNull GoalType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 8, fraction = 2, message = "La valeur cible accepte au maximum 8 chiffres entiers et 2 décimales")
        BigDecimal targetValue,
        @NotBlank @Size(max = 24) String unit,
        @NotNull LocalDate startDate,
        @NotNull LocalDate targetDate,
        @NotNull GoalStatus status
) {
    @AssertTrue(message = "La date cible doit être égale ou postérieure à la date de début")
    public boolean isTargetDateOnOrAfterStartDate() {
        return startDate == null || targetDate == null || !targetDate.isBefore(startDate);
    }
}
