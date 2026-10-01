package com.aardranpk.triageboard.incident;

import java.time.Instant;

import com.aardranpk.triageboard.analyst.Analyst;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        IncidentStatus status,
        Severity severity,
        Long assigneeId,
        String assigneeName,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {

    public static IncidentResponse from(Incident incident) {
        Analyst assignee = incident.getAssignee();
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getSeverity(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getClosedAt()
        );
    }
}