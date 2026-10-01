package com.aardranpk.triageboard.triage;

import java.time.Instant;

import com.aardranpk.triageboard.incident.Incident;
import com.aardranpk.triageboard.incident.Severity;

public record TriageEntry(Long incidentId, String title, Severity severity, Instant createdAt) {

    public static TriageEntry from(Incident incident) {
        return new TriageEntry(incident.getId(), incident.getTitle(),
                incident.getSeverity(), incident.getCreatedAt());
    }
}