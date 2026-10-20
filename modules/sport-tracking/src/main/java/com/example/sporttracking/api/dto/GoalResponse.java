package com.example.sporttracking.api.dto;

import com.example.sporttracking.goal.Goal;
import com.example.sporttracking.goal.GoalStatus;
import com.example.sporttracking.goal.GoalType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(
        Long id,
        Long userId,
        GoalType type,
        BigDecimal targetValue,
        String unit,
        LocalDate startDate,
        LocalDate targetDate,
        GoalStatus status
) {
    public static GoalResponse from(Goal goal) {
        return new GoalResponse(goal.getId(), goal.getUserId(), goal.getType(), goal.getTargetValue(),
                goal.getUnit(), goal.getStartDate(), goal.getTargetDate(), goal.getStatus());
    }
}
