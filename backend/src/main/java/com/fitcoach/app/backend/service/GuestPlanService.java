package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.GuestPlanRequest;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestPlanService {
    private final FitnessCalculationsService fitnessCalculationsService;
    private final AiPlanService aiPlanService;

    public WorkoutPlan generate(GuestPlanRequest request) {
        UserProfile profile = new UserProfile();
        profile.setFirstName(request.getFirstName());
        profile.setLastName("Guest");
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setHeightCm(request.getHeightCm());
        profile.setWeightKg(request.getWeightKg());
        profile.setFitnessLevel(request.getFitnessLevel());
        profile.setTrainingExperience(request.getTrainingExperience());
        profile.setTrainingDaysPerWeek(request.getTrainingDaysPerWeek());
        profile.setSessionDurationMinutes(request.getSessionDurationMinutes());
        profile.setTrainingLocation(request.getTrainingLocation());
        profile.setEquipment(request.getEquipment());
        profile.setMainGoal(request.getMainGoal());
        profile.setTargetWeightKg(request.getTargetWeightKg());
        fitnessCalculationsService.calculateProfileMetrics(profile);
        return aiPlanService.generateGuestPlan(profile, request.getInjuriesOrLimitations());
    }
}
