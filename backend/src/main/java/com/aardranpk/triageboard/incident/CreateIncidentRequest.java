package com.aardranpk.triageboard.incident;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        Severity severity,
        @DecimalMin("0.0") @DecimalMax("1.0") Double detectionConfidence
) {
}