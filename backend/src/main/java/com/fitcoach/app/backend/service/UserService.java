package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.ProfileRequest;
import com.fitcoach.app.backend.dto.ProfileResponse;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.repository.UserProfileRepository;
import com.fitcoach.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FitnessCalculationsService calculationsService;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public ProfileResponse getProfile() {
        User user = getCurrentUser();
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new IllegalArgumentException("Profile not yet created");
        }

        return ProfileResponse.builder()
                .id(user.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(user.getEmail())
                .age(profile.getAge())
                .gender(profile.getGender())
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .fitnessLevel(profile.getFitnessLevel())
                .trainingExperience(profile.getTrainingExperience())
                .trainingDaysPerWeek(profile.getTrainingDaysPerWeek())
                .sessionDurationMinutes(profile.getSessionDurationMinutes())
                .trainingLocation(profile.getTrainingLocation())
                .equipment(profile.getEquipment())
                .mainGoal(profile.getMainGoal())
                .targetWeightKg(profile.getTargetWeightKg())
                .targetDate(profile.getTargetDate())
                .bmi(profile.getBmi())
                .bmr(profile.getBmr())
                .tdee(profile.getTdee())
                .calorieTarget(profile.getCalorieTarget())
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(ProfileRequest request) {
        User user = getCurrentUser();
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setProfile(profile);
        }

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getHeightCm() != null) profile.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) profile.setWeightKg(request.getWeightKg());
        if (request.getFitnessLevel() != null) profile.setFitnessLevel(request.getFitnessLevel());
        if (request.getTrainingExperience() != null) profile.setTrainingExperience(request.getTrainingExperience());
        if (request.getTrainingDaysPerWeek() != null) profile.setTrainingDaysPerWeek(request.getTrainingDaysPerWeek());
        if (request.getSessionDurationMinutes() != null) profile.setSessionDurationMinutes(request.getSessionDurationMinutes());
        if (request.getTrainingLocation() != null) profile.setTrainingLocation(request.getTrainingLocation());
        if (request.getEquipment() != null) profile.setEquipment(request.getEquipment());
        if (request.getMainGoal() != null) profile.setMainGoal(request.getMainGoal());
        if (request.getTargetWeightKg() != null) profile.setTargetWeightKg(request.getTargetWeightKg());
        if (request.getTargetDate() != null) profile.setTargetDate(request.getTargetDate());

        calculationsService.calculateProfileMetrics(profile);

        userRepository.save(user);
        return getProfile();
    }
}
