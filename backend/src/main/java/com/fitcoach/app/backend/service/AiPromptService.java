package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class AiPromptService {

    public String buildPlanPrompt(User user, UserProfile profile) {
        return buildPlanPrompt(profile, null);
    }

    public String buildPlanPrompt(UserProfile profile, String injuriesOrLimitations) {
        return """
            You are a careful, evidence-informed fitness coach. Generate a precise structured JSON plan for a 30-day fitness program.
            Requirements:
            - Return strict JSON only with no markdown fences.
            - Use fields: summary, weeklyStrategy, nutritionGuidance, weeks.
            - Each week has weekNumber, focus, days.
            - Each day has day, type, exercises[] and notes.
            - Each exercise has name, sets, reps, restSeconds, notes.
            - Keep workouts safe and realistic.
            - User profile: age=%s, gender=%s, heightCm=%s, weightKg=%s, bmi=%s, bmr=%s, tdee=%s, fitnessLevel=%s, trainingExperience=%s, trainingDaysPerWeek=%s, sessionDurationMinutes=%s, trainingLocation=%s, equipment=%s, mainGoal=%s, targetWeightKg=%s, targetDate=%s, limitations=%s.
            - Do not diagnose medical conditions. If there are injury concerns, recommend professional guidance.
            - Ensure a progressive adaptation over the month.
            - Include rest days and training intensity guidance.
            - Adapt every recommendation to the user's available time, location, equipment, experience, goal, and limitations.
            - Never prescribe through pain. If limitations are present, include safer substitutions and recommend professional guidance.
        """.formatted(
                profile.getAge(),
                profile.getGender(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getBmi(),
                profile.getBmr(),
                profile.getTdee(),
                profile.getFitnessLevel(),
                profile.getTrainingExperience(),
                profile.getTrainingDaysPerWeek(),
                profile.getSessionDurationMinutes(),
                profile.getTrainingLocation(),
                profile.getEquipment(),
                profile.getMainGoal(),
                profile.getTargetWeightKg(),
                profile.getTargetDate(),
                injuriesOrLimitations == null || injuriesOrLimitations.isBlank() ? "None reported" : injuriesOrLimitations
        );
    }
}
