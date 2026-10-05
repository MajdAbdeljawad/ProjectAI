package com.fitcoach.app.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GuestPlanRequest {
    @NotBlank
    private String firstName;

    @NotNull
    @Min(16)
    @Max(100)
    private Integer age;

    @NotBlank
    private String gender;

    @NotNull
    @Min(100)
    @Max(250)
    private Double heightCm;

    @NotNull
    @Min(30)
    @Max(300)
    private Double weightKg;

    @NotBlank
    private String fitnessLevel;

    @NotBlank
    private String trainingExperience;

    @NotNull
    @Min(0)
    @Max(7)
    private Integer trainingDaysPerWeek;

    @NotNull
    @Min(15)
    @Max(240)
    private Integer sessionDurationMinutes;

    @NotBlank
    private String trainingLocation;

    @NotBlank
    private String equipment;

    @NotBlank
    private String mainGoal;

    private Double targetWeightKg;
    private String injuriesOrLimitations;
}
