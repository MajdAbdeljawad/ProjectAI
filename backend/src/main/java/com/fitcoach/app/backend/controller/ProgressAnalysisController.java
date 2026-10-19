package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.dto.ProgressAnalysisRequest;
import com.fitcoach.app.backend.dto.ProgressAnalysisResponse;
import com.fitcoach.app.backend.service.ProgressAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/progress")
@RequiredArgsConstructor
public class ProgressAnalysisController {
    private final ProgressAnalysisService progressAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<ProgressAnalysisResponse> analyze(@Valid @RequestBody ProgressAnalysisRequest request) {
        return ResponseEntity.ok(progressAnalysisService.analyze(request));
    }
}
