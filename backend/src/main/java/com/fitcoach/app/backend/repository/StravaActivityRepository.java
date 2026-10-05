package com.fitcoach.app.backend.repository;

import com.fitcoach.app.backend.entity.StravaActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StravaActivityRepository extends JpaRepository<StravaActivity, Long> {
    Optional<StravaActivity> findByUserIdAndExternalId(Long userId, String externalId);
    List<StravaActivity> findByUserIdOrderByStartDateDesc(Long userId);
}
