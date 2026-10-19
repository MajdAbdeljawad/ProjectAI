package com.fitcoach.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCheckinResponse {
    private Long id;
    private LocalDate recordedOn;
    private LocalDateTime recordedAt;
    private Double sleepHours;
    private Integer sleepQuality;
    private Integer fatigueLevel;
    private Integer sorenessLevel;
    private String sorenessAreas;
    private Integer restingHeartRate;
    private String symptoms;
    private String limitations;
    private String notes;
    private String source;
    @Builder.Default
    private List<String> normalizationNotes = new ArrayList<>();
}
