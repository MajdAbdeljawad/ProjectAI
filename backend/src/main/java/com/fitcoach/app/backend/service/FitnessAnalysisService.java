package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.StravaActivity;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.repository.StravaActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FitnessAnalysisService {

    private final StravaActivityRepository stravaActivityRepository;
    private final UserService userService;

    public String summarizeRecentActivity() {
        User user = userService.getCurrentUser();
        List<StravaActivity> activities = stravaActivityRepository.findByUserIdOrderByStartDateDesc(user.getId());

        double runningDistance = activities.stream()
                .filter(a -> "Run".equalsIgnoreCase(a.getType()))
                .mapToDouble(a -> a.getDistanceMeters() != null ? a.getDistanceMeters() : 0)
                .sum();

        double cyclingDistance = activities.stream()
                .filter(a -> "Ride".equalsIgnoreCase(a.getType()))
                .mapToDouble(a -> a.getDistanceMeters() != null ? a.getDistanceMeters() : 0)
                .sum();

        long totalMinutes = activities.stream()
                .mapToLong(a -> a.getDurationSeconds() != null ? a.getDurationSeconds() / 60 : 0)
                .sum();

        return String.format(
                "Recent activity summary: %d activities, running %.1f km, cycling %.1f km, total duration %d minutes.",
                activities.size(),
                runningDistance,
                cyclingDistance,
                totalMinutes
        );
    }

    public boolean userIsOverTraining() {
        User user = userService.getCurrentUser();
        List<StravaActivity> activities = stravaActivityRepository.findByUserIdOrderByStartDateDesc(user.getId());
        long recentMinutes = activities.stream()
                .filter(a -> a.getStartDate() != null && a.getStartDate().isAfter(LocalDateTime.now().minusDays(7)))
                .mapToLong(a -> a.getDurationSeconds() != null ? a.getDurationSeconds() / 60 : 0)
                .sum();
        return recentMinutes > 300;
    }
}
