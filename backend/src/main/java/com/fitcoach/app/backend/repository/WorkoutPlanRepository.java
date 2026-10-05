package com.fitcoach.app.backend.repository;

import com.fitcoach.app.backend.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    List<WorkoutPlan> findByUserIdOrderByGeneratedAtDesc(Long userId);
    Optional<WorkoutPlan> findFirstByUserIdOrderByGeneratedAtDesc(Long userId);
}
