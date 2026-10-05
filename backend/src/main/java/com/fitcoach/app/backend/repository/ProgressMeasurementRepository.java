package com.fitcoach.app.backend.repository;

import com.fitcoach.app.backend.entity.ProgressMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressMeasurementRepository extends JpaRepository<ProgressMeasurement, Long> {
    List<ProgressMeasurement> findByUserIdOrderByMeasuredOnDesc(Long userId);
}
