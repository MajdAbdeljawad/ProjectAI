package com.fitcoach.app.backend.repository;

import com.fitcoach.app.backend.entity.StravaAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StravaAccountRepository extends JpaRepository<StravaAccount, Long> {
    Optional<StravaAccount> findByUserId(Long userId);
}
