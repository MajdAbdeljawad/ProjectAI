package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import com.fitcoach.app.backend.repository.WorkoutPlanRepository;
import com.fitcoach.app.backend.service.AiPlanService;
import com.fitcoach.app.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanController {

    private final UserService userService;
    private final AiPlanService aiPlanService;
    private final WorkoutPlanRepository workoutPlanRepository;

    @PostMapping("/plans/generate")
    public ResponseEntity<WorkoutPlan> generatePlan() {
        User user = userService.getCurrentUser();
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new IllegalStateException("Profile must be completed before generating a plan");
        }
        return ResponseEntity.ok(aiPlanService.generateForUser(user, profile));
    }

    @GetMapping("/plans/current")
    public ResponseEntity<WorkoutPlan> currentPlan() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(workoutPlanRepository.findFirstByUserIdOrderByGeneratedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalStateException("No plan generated yet")));
    }

    @GetMapping("/plans")
    public ResponseEntity<List<WorkoutPlan>> listPlans() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(workoutPlanRepository.findByUserIdOrderByGeneratedAtDesc(user.getId()));
    }
}
