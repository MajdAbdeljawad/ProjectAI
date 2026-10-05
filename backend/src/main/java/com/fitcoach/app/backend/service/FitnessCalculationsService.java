package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class FitnessCalculationsService {

    public void calculateProfileMetrics(UserProfile profile) {
        double heightM = profile.getHeightCm() != null ? profile.getHeightCm() / 100.0 : 1.75;
        double weightKg = profile.getWeightKg() != null ? profile.getWeightKg() : 70.0;
        double age = profile.getAge() != null ? profile.getAge() : 30;
        String gender = profile.getGender() != null ? profile.getGender().toLowerCase() : "male";

        double bmi = weightKg / (heightM * heightM);
        double bmr = calculateBmr(weightKg, heightM * 100.0, age, gender);
        double tdee = calculateTdee(bmr, profile.getTrainingDaysPerWeek() != null ? profile.getTrainingDaysPerWeek() : 3);
        double calorieTarget = calculateCalorieTarget(tdee, profile.getMainGoal());

        profile.setBmi(round(bmi));
        profile.setBmr(round(bmr));
        profile.setTdee(round(tdee));
        profile.setCalorieTarget(round(calorieTarget));
    }

    private double calculateBmr(double weightKg, double heightCm, double age, String gender) {
        double bmr;
        if ("female".equals(gender)) {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age - 161;
        } else {
            bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
        }
        return bmr;
    }

    private double calculateTdee(double bmr, int trainingDays) {
        double activityMultiplier = switch (trainingDays) {
            case 0 -> 1.2;
            case 1 -> 1.375;
            case 2 -> 1.45;
            case 3 -> 1.55;
            case 4 -> 1.7;
            case 5 -> 1.8;
            default -> 1.9;
        };
        return bmr * activityMultiplier;
    }

    private double calculateCalorieTarget(double tdee, String goal) {
        if (goal == null) return tdee;
        return switch (goal.toLowerCase()) {
            case "fat loss", "weight loss" -> Math.max(1800, tdee - 300);
            case "muscle gain", "bulking" -> tdee + 250;
            case "strength improvement" -> tdee + 150;
            case "endurance improvement" -> tdee + 100;
            case "weight maintenance", "maintenance" -> tdee;
            default -> tdee;
        };
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
