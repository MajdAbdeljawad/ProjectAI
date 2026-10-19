package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecoveryCheckinRequest;
import com.fitcoach.app.backend.dto.RecoveryCheckinResponse;
import com.fitcoach.app.backend.entity.RecoveryCheckin;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.repository.RecoveryCheckinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecoveryCheckinService {

    private final RecoveryCheckinRepository recoveryCheckinRepository;
    private final UserService userService;

    @Transactional
    public RecoveryCheckinResponse save(RecoveryCheckinRequest request) {
        return saveForUser(userService.getCurrentUser(), request);
    }

    @Transactional
    public RecoveryCheckinResponse saveForUser(User user, RecoveryCheckinRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate recordedOn = request.getRecordedOn() == null ? today : request.getRecordedOn();
        RecoveryDataValidator.rejectFutureDate(recordedOn, today);

        RecoveryDataValidator.NormalizedSleep sleep = RecoveryDataValidator.normalizeSleep(request.getSleepHours());
        List<String> notes = new ArrayList<>(sleep.notes());
        notes.addAll(RecoveryDataValidator.inspectHeartRate(request.getRestingHeartRate()));
        notes.addAll(RecoveryDataValidator.detectSubjectiveContradictions(
                sleep.hours(),
                request.getFatigueLevel(),
                request.getSorenessLevel(),
                request.getNotes(),
                request.getSymptoms()
        ));

        RecoveryCheckin checkin = new RecoveryCheckin();
        checkin.setUser(user);
        checkin.setRecordedOn(recordedOn);
        checkin.setRecordedAt(LocalDateTime.now());
        checkin.setSleepHours(sleep.hours());
        checkin.setSleepQuality(request.getSleepQuality());
        checkin.setFatigueLevel(request.getFatigueLevel());
        checkin.setSorenessLevel(request.getSorenessLevel());
        checkin.setSorenessAreas(blankToNull(request.getSorenessAreas()));
        checkin.setRestingHeartRate(request.getRestingHeartRate());
        checkin.setSymptoms(blankToNull(request.getSymptoms()));
        checkin.setLimitations(blankToNull(request.getLimitations()));
        checkin.setNotes(blankToNull(request.getNotes()));
        checkin.setSource("SELF_REPORT");

        RecoveryCheckin saved = recoveryCheckinRepository.save(checkin);
        RecoveryCheckinResponse response = toResponse(saved);
        response.setNormalizationNotes(notes);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RecoveryCheckinResponse> listCurrentUser() {
        User user = userService.getCurrentUser();
        return recoveryCheckinRepository.findByUserIdOrderByRecordedOnDescRecordedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RecoveryCheckinResponse toResponse(RecoveryCheckin checkin) {
        return RecoveryCheckinResponse.builder()
                .id(checkin.getId())
                .recordedOn(checkin.getRecordedOn())
                .recordedAt(checkin.getRecordedAt())
                .sleepHours(checkin.getSleepHours())
                .sleepQuality(checkin.getSleepQuality())
                .fatigueLevel(checkin.getFatigueLevel())
                .sorenessLevel(checkin.getSorenessLevel())
                .sorenessAreas(checkin.getSorenessAreas())
                .restingHeartRate(checkin.getRestingHeartRate())
                .symptoms(checkin.getSymptoms())
                .limitations(checkin.getLimitations())
                .notes(checkin.getNotes())
                .source(checkin.getSource())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
