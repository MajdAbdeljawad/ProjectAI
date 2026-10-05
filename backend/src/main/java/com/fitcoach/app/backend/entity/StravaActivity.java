package com.fitcoach.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "strava_activities", indexes = {
        @Index(name = "idx_strava_activity_user_external", columnList = "user_id, external_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class StravaActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String type;

    private String name;
    private LocalDateTime startDate;
    private Long durationSeconds;
    private Double distanceMeters;
    private Double averageSpeed;
    private Double elevationGain;
    private Double calories;
    private Double averageHeartRate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
