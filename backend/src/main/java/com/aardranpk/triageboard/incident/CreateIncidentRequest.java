package com.aardranpk.triageboard.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        Severity severity
) {
}