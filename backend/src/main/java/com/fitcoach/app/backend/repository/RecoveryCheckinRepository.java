package com.fitcoach.app.backend.repository;

import com.fitcoach.app.backend.entity.RecoveryCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecoveryCheckinRepository extends JpaRepository<RecoveryCheckin, Long> {
    List<RecoveryCheckin> findByUserIdOrderByRecordedOnDescRecordedAtDesc(Long userId);

    List<RecoveryCheckin> findByUserIdAndRecordedOnGreaterThanEqualOrderByRecordedOnAscRecordedAtAsc(
            Long userId, LocalDate from);
}
