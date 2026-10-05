package com.fitcoach.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
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

    private Double bmi;
    private Double bmr;
    private Double tdee;
    private Double calorieTarget;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
