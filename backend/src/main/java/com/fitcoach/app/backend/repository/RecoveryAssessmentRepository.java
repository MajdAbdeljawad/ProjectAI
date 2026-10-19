package com.fitcoach.app.backend.repository;

import com.fitcoach.app.backend.entity.RecoveryAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecoveryAssessmentRepository extends JpaRepository<RecoveryAssessment, Long> {
    List<RecoveryAssessment> findByUserIdOrderByGeneratedAtDesc(Long userId);

    Optional<RecoveryAssessment> findFirstByUserIdOrderByGeneratedAtDesc(Long userId);

    Optional<RecoveryAssessment> findByIdAndUserId(Long id, Long userId);
}
