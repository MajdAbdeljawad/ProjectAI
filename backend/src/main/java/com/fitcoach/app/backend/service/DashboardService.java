package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.DashboardResponse;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import com.fitcoach.app.backend.repository.StravaActivityRepository;
import com.fitcoach.app.backend.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserService userService;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final StravaActivityRepository stravaActivityRepository;

    public DashboardResponse getDashboard() {
        User user = userService.getCurrentUser();
        UserProfile profile = user.getProfile();
        WorkoutPlan plan = workoutPlanRepository.findFirstByUserIdOrderByGeneratedAtDesc(user.getId()).orElse(null);
        List<?> activities = stravaActivityRepository.findByUserIdOrderByStartDateDesc(user.getId());

        String goal = profile != null && profile.getMainGoal() != null ? profile.getMainGoal() : "General fitness";
        double weight = profile != null && profile.getWeightKg() != null ? profile.getWeightKg() : 0.0;
        double targetWeight = profile != null && profile.getTargetWeightKg() != null ? profile.getTargetWeightKg() : weight;
        double bmi = profile != null && profile.getBmi() != null ? profile.getBmi() : 0.0;

        return DashboardResponse.builder()
                .firstName(profile != null ? profile.getFirstName() : user.getEmail())
                .goal(goal)
                .currentWeight(weight)
                .targetWeight(targetWeight)
                .bmi(bmi)
                .todayWorkout("Full-body strength")
                .planCompletionPercent(plan != null ? 80 : 0)
                .stravaActivitiesThisWeek(activities.size())
                .aiCoachMessage("You're progressing well. Keep your training volume steady and recover well.")
                .build();
    }
}
