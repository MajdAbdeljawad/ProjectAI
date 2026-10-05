package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.entity.ProgressMeasurement;
import com.fitcoach.app.backend.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/progress")
    public ResponseEntity<List<ProgressMeasurement>> getProgress() {
        return ResponseEntity.ok(progressService.getProgress());
    }

    @PostMapping("/progress")
    public ResponseEntity<ProgressMeasurement> addProgress(@RequestParam Double weightKg,
                                                         @RequestParam(required = false) Double bmi,
                                                         @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(progressService.addMeasurement(weightKg, bmi, notes));
    }
}
