package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.dto.GenerateRecoveryRequest;
import com.fitcoach.app.backend.dto.RecoveryAssessmentResponse;
import com.fitcoach.app.backend.dto.RecoveryCheckinRequest;
import com.fitcoach.app.backend.dto.RecoveryCheckinResponse;
import com.fitcoach.app.backend.dto.RecoveryOverviewResponse;
import com.fitcoach.app.backend.service.RecoveryAssessmentService;
import com.fitcoach.app.backend.service.RecoveryCheckinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
public class RecoveryController {

    private final RecoveryCheckinService recoveryCheckinService;
    private final RecoveryAssessmentService recoveryAssessmentService;

    @PostMapping("/checkins")
    public ResponseEntity<RecoveryCheckinResponse> createCheckin(@Valid @RequestBody RecoveryCheckinRequest request) {
        return ResponseEntity.ok(recoveryCheckinService.save(request));
    }

    @GetMapping("/checkins")
    public ResponseEntity<List<RecoveryCheckinResponse>> listCheckins() {
        return ResponseEntity.ok(recoveryCheckinService.listCurrentUser());
    }

    @GetMapping("/overview")
    public ResponseEntity<RecoveryOverviewResponse> overview() {
        return ResponseEntity.ok(recoveryAssessmentService.overview());
    }

    @PostMapping("/assessments")
    public ResponseEntity<RecoveryAssessmentResponse> generate(
            @Valid @RequestBody(required = false) GenerateRecoveryRequest request) {
        return ResponseEntity.ok(recoveryAssessmentService.generate(request));
    }

    @GetMapping("/assessments")
    public ResponseEntity<List<RecoveryAssessmentResponse>> history() {
        return ResponseEntity.ok(recoveryAssessmentService.history());
    }

    @GetMapping("/assessments/{id}")
    public ResponseEntity<RecoveryAssessmentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(recoveryAssessmentService.getOwned(id));
    }
}
