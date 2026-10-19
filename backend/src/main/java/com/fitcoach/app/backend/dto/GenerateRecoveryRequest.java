package com.fitcoach.app.backend.dto;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class GenerateRecoveryRequest {
    @Valid
    private RecoveryCheckinRequest checkin;
}
