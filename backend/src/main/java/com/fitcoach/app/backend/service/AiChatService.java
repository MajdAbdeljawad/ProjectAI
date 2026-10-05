package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final UserService userService;
    private final FitnessAnalysisService fitnessAnalysisService;

    public AiChatService(UserService userService, FitnessAnalysisService fitnessAnalysisService) {
        this.userService = userService;
        this.fitnessAnalysisService = fitnessAnalysisService;
    }

    public String buildContext() {
        User user = userService.getCurrentUser();
        UserProfile profile = user.getProfile();
        String name = profile != null && profile.getFirstName() != null ? profile.getFirstName() : user.getEmail();
        String goal = profile != null && profile.getMainGoal() != null ? profile.getMainGoal() : "General fitness";
        String activitySummary = fitnessAnalysisService.summarizeRecentActivity();
        return "User: " + name + ". Goal: " + goal + ". " + activitySummary;
    }

    public String answer(String question) {
        String base = buildContext();
        if (question.toLowerCase().contains("missed") || question.toLowerCase().contains("skipped")) {
            return "Return to your plan with a lighter session rather than making up everything at once. Focus on recovery and keep consistency high.";
        }
        if (question.toLowerCase().contains("eat")) {
            return "Aim for balanced meals with protein, vegetables, complex carbs, and healthy fats around your training window.";
        }
        if (question.toLowerCase().contains("30 minutes")) {
            return "Use a 30-minute session focused on compound work, short rests, and one finish set. Prioritize quality and consistency.";
        }
        return "Based on your profile and recent activity, the safest next step is consistency. Keep training at moderate intensity and use your recovery data to adjust volume." + " Context: " + base;
    }
}
