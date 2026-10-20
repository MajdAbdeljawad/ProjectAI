package com.fitcoach.app.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_checkins")
@Getter
@Setter
@NoArgsConstructor
public class RecoveryCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private LocalDate recordedOn;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    private Double sleepHours;
    private Integer sleepQuality;
    private Integer fatigueLevel;
    private Integer sorenessLevel;
    private String sorenessAreas;
    private Integer restingHeartRate;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String limitations;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private String source = "SELF_REPORT";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (recordedAt == null) {
            recordedAt = createdAt;
        }
        if (source == null || source.isBlank()) {
            source = "SELF_REPORT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
