package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.service.StravaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/strava")
@RequiredArgsConstructor
public class StravaController {

    private final StravaService stravaService;

    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connect() {
        return ResponseEntity.ok(Map.of("url", stravaService.getConnectUrl()));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        return ResponseEntity.ok(stravaService.handleCallback(code));
    }

    @PostMapping("/sync")
    public ResponseEntity<String> sync() {
        stravaService.syncActivities();
        return ResponseEntity.ok("Synchronization complete");
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<String> disconnect() {
        stravaService.disconnect();
        return ResponseEntity.ok("Disconnected");
    }
}
