package com.example.sporttracking.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgressEntryRepository extends JpaRepository<ProgressEntry, Long> {
    List<ProgressEntry> findByUserIdOrderByMeasurementDateDesc(Long userId);
    Optional<ProgressEntry> findByIdAndUserId(Long id, Long userId);
}
