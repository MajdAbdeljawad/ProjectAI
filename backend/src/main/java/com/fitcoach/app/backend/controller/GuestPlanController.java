package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.dto.GuestPlanRequest;
import com.fitcoach.app.backend.entity.WorkoutPlan;
import com.fitcoach.app.backend.service.GuestPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class GuestPlanController {
    private final GuestPlanService guestPlanService;

    @PostMapping("/plans/generate")
    public ResponseEntity<WorkoutPlan> generate(@Valid @RequestBody GuestPlanRequest request) {
        return ResponseEntity.ok(guestPlanService.generate(request));
    }
}
