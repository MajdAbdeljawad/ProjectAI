package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String gender;
    private Double heightCm;
    private Double weightKg;
    private String fitnessLevel;
    private String trainingExperience;
    private Integer trainingDaysPerWeek;
    private Integer sessionDurationMinutes;
    private String trainingLocation;
    private String equipment;
    private String mainGoal;
    private Double targetWeightKg;
    private LocalDate targetDate;
    private Double bmi;
    private Double bmr;
    private Double tdee;
    private Double calorieTarget;
}
