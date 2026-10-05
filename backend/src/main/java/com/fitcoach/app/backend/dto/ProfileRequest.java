package com.fitcoach.app.backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileRequest {
    private String firstName;
    private String lastName;
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
}
