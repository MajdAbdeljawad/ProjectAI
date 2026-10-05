package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String firstName;
    private String goal;
    private Double currentWeight;
    private Double targetWeight;
    private Double bmi;
    private String todayWorkout;
    private Integer planCompletionPercent;
    private Integer stravaActivitiesThisWeek;
    private String aiCoachMessage;
}
