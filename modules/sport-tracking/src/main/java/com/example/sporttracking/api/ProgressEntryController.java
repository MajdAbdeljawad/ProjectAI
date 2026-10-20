package com.example.sporttracking.api;

import com.example.sporttracking.api.dto.ProgressEntryRequest;
import com.example.sporttracking.api.dto.ProgressEntryResponse;
import com.example.sporttracking.progress.ProgressEntry;
import com.example.sporttracking.progress.ProgressEntryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/progress-entries")
@Validated
public class ProgressEntryController {
    private final ProgressEntryRepository progressEntryRepository;

    public ProgressEntryController(ProgressEntryRepository progressEntryRepository) {
        this.progressEntryRepository = progressEntryRepository;
    }

    @GetMapping
    public List<ProgressEntryResponse> list(@PathVariable @Positive Long userId) {
        return progressEntryRepository.findByUserIdOrderByMeasurementDateDesc(userId).stream()
                .map(ProgressEntryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ProgressEntryResponse> create(@PathVariable @Positive Long userId,
                                                        @Valid @RequestBody ProgressEntryRequest request,
                                                        UriComponentsBuilder uriBuilder) {
        ProgressEntry entry = toEntity(userId, request);
        ProgressEntryResponse response = ProgressEntryResponse.from(progressEntryRepository.save(entry));
        var location = uriBuilder.path("/api/users/{userId}/progress-entries/{entryId}")
                .buildAndExpand(userId, response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{entryId}")
    public ProgressEntryResponse get(@PathVariable @Positive Long userId,
                                     @PathVariable @Positive Long entryId) {
        return ProgressEntryResponse.from(findEntry(userId, entryId));
    }

    @PutMapping("/{entryId}")
    public ProgressEntryResponse update(@PathVariable @Positive Long userId,
                                       @PathVariable @Positive Long entryId,
                                       @Valid @RequestBody ProgressEntryRequest request) {
        ProgressEntry entry = findEntry(userId, entryId);
        entry.setMeasurementDate(request.measurementDate());
        entry.setWeight(request.weight());
        entry.setWaistCircumference(request.waistCircumference());
        entry.setSleepHours(request.sleepHours());
        entry.setEnergyLevel(request.energyLevel());
        return ProgressEntryResponse.from(progressEntryRepository.save(entry));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long userId,
                                       @PathVariable @Positive Long entryId) {
        progressEntryRepository.delete(findEntry(userId, entryId));
        return ResponseEntity.noContent().build();
    }

    private ProgressEntry findEntry(Long userId, Long entryId) {
        return progressEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée de progression"));
    }

    private ProgressEntry toEntity(Long userId, ProgressEntryRequest request) {
        return ProgressEntry.builder()
                .userId(userId)
                .measurementDate(request.measurementDate())
                .weight(request.weight())
                .waistCircumference(request.waistCircumference())
                .sleepHours(request.sleepHours())
                .energyLevel(request.energyLevel())
                .build();
    }
}
