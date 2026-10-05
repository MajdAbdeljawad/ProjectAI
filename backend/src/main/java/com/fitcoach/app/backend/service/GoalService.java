package com.fitcoach.app.backend.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalService {
    public List<String> getAvailableGoals() {
        return List.of(
                "Muscle gain",
                "Fat loss",
                "Weight maintenance",
                "Strength improvement",
                "Endurance improvement",
                "General fitness"
        );
    }
}
