package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.ProgressMeasurement;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.repository.ProgressMeasurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressMeasurementRepository progressMeasurementRepository;
    private final UserService userService;

    public List<ProgressMeasurement> getProgress() {
        User user = userService.getCurrentUser();
        return progressMeasurementRepository.findByUserIdOrderByMeasuredOnDesc(user.getId());
    }

    public ProgressMeasurement addMeasurement(Double weightKg, Double bmi, String notes) {
        User user = userService.getCurrentUser();
        ProgressMeasurement measurement = new ProgressMeasurement();
        measurement.setUser(user);
        measurement.setMeasuredOn(LocalDate.now());
        measurement.setWeightKg(weightKg);
        measurement.setBmi(bmi);
        measurement.setNotes(notes);
        return progressMeasurementRepository.save(measurement);
    }
}
