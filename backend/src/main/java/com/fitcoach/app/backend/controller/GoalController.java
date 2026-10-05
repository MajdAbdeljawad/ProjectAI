package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping("/goals")
    public ResponseEntity<List<String>> getGoals() {
        return ResponseEntity.ok(goalService.getAvailableGoals());
    }
}
