package com.aardranpk.triageboard.incident;

import jakarta.validation.constraints.NotNull;

public record AssignIncidentRequest(@NotNull Long analystId) {
}