package com.example.backend_meal.nutrition.service;

import com.example.backend_meal.nutrition.model.NutritionProfile;
import com.example.backend_meal.nutrition.repository.NutritionProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NutritionProfileService {

    private final NutritionProfileRepository profileRepository;
    private final NutritionCalculatorService calculator;

    public NutritionProfileService(NutritionProfileRepository profileRepository,
                                   NutritionCalculatorService calculator) {
        this.profileRepository = profileRepository;
        this.calculator = calculator;
    }

    /**
     * Creates or updates profile by userId.
     * If a profile with same userId exists, updates it.
     * Recalculates nutrition targets before saving.
     */
    public NutritionProfile createOrUpdate(NutritionProfile incoming) {
        // Calculate nutrition targets
        calculator.calculate(incoming);

        if (incoming.getUserId() != null && !incoming.getUserId().isBlank()) {
            Optional<NutritionProfile> existing = profileRepository.findByUserId(incoming.getUserId());
            if (existing.isPresent()) {
                NutritionProfile saved = existing.get();
                return updateFields(saved, incoming);
            }
        }
        return profileRepository.save(incoming);
    }

    /**
     * PUT /profiles/{id} — updates an existing profile by database ID.
     */
    public NutritionProfile update(Long id, NutritionProfile updates) {
        NutritionProfile existing = profileRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Profil introuvable: id=" + id));
        // updateFields already calls calculator.calculate() and saves
        return updateFields(existing, updates);
    }

    @Transactional(readOnly = true)
    public Optional<NutritionProfile> findById(Long id) {
        return profileRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<NutritionProfile> findByUserId(String userId) {
        return profileRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<NutritionProfile> findAll() {
        return profileRepository.findAll();
    }

    /** Copies all modifiable fields from source to target, then recalculates. */
    private NutritionProfile updateFields(NutritionProfile target, NutritionProfile source) {
        if (source.getFullName()        != null) target.setFullName(source.getFullName());
        if (source.getAge()             != null) target.setAge(source.getAge());
        if (source.getSex()             != null) target.setSex(source.getSex());
        if (source.getHeight()          != null) target.setHeight(source.getHeight());
        if (source.getWeight()          != null) target.setWeight(source.getWeight());
        if (source.getGoal()            != null) target.setGoal(source.getGoal());
        if (source.getSportsLevel()     != null) target.setSportsLevel(source.getSportsLevel());
        if (source.getSessionsPerWeek() != null) target.setSessionsPerWeek(source.getSessionsPerWeek());
        if (source.getWorkoutType()     != null) target.setWorkoutType(source.getWorkoutType());
        if (source.getDiet()            != null) target.setDiet(source.getDiet());
        if (source.getMealsPerDay()     != null) target.setMealsPerDay(source.getMealsPerDay());
        if (source.getWakeUpTime()      != null) target.setWakeUpTime(source.getWakeUpTime());
        if (source.getSleepTime()       != null) target.setSleepTime(source.getSleepTime());
        if (source.getBudget()          != null) target.setBudget(source.getBudget());
        if (source.getCookingTime()     != null) target.setCookingTime(source.getCookingTime());
        if (source.getUserId()          != null) target.setUserId(source.getUserId());

        if (source.getAllergies()            != null) target.setAllergies(source.getAllergies());
        if (source.getIntolerances()        != null) target.setIntolerances(source.getIntolerances());
        if (source.getPreferredFoods()      != null) target.setPreferredFoods(source.getPreferredFoods());
        if (source.getDislikedFoods()       != null) target.setDislikedFoods(source.getDislikedFoods());
        if (source.getDietaryRestrictions() != null) target.setDietaryRestrictions(source.getDietaryRestrictions());

        // Recalculate targets after field update
        calculator.calculate(target);
        return profileRepository.save(target);
    }
}
