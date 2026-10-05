package com.aardranpk.triageboard.incident;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;
import com.aardranpk.triageboard.common.AfterCommit;
import com.aardranpk.triageboard.common.InvalidStateException;
import com.aardranpk.triageboard.common.NotFoundException;
import com.aardranpk.triageboard.scoring.ScoringClient;
import com.aardranpk.triageboard.triage.TriageEntry;
import com.aardranpk.triageboard.triage.TriageQueue;
import com.aardranpk.triageboard.triage.WorkloadCache;

@Service
@Transactional
public class IncidentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final IncidentRepository incidentRepository;
    private final AnalystRepository analystRepository;
    private final TriageQueue triageQueue;
    private final WorkloadCache workloadCache;
    private final ScoringClient scoringClient;

    public IncidentService(IncidentRepository incidentRepository,
                           AnalystRepository analystRepository,
                           TriageQueue triageQueue,
                           WorkloadCache workloadCache,
                           ScoringClient scoringClient) {
        this.incidentRepository = incidentRepository;
        this.analystRepository = analystRepository;
        this.triageQueue = triageQueue;
        this.workloadCache = workloadCache;
        this.scoringClient = scoringClient;
    }

    public IncidentResponse create(CreateIncidentRequest request) {
        Incident incident = new Incident(request.title(), request.description());
        incident.setDetectionConfidence(request.detectionConfidence());
        applySeverity(incident, request);
        autoAssignTarget().ifPresent(incident::assignTo);

        Incident saved = incidentRepository.save(incident);

        TriageEntry entry = TriageEntry.from(saved);
        Long assigneeId = assigneeIdOf(saved);
        AfterCommit.run(() -> {
            triageQueue.upsert(entry);
            if (assigneeId != null) {
                workloadCache.increment(assigneeId);
            }
        });
        return IncidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        return IncidentResponse.from(findIncident(id));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(IncidentStatus status) {
        List<Incident> incidents = (status == null)
                ? incidentRepository.findAll(NEWEST_FIRST)
                : incidentRepository.findByStatus(status, NEWEST_FIRST);
        return incidents.stream().map(IncidentResponse::from).toList();
    }

    public IncidentResponse assign(Long incidentId, Long analystId) {
        Incident incident = findIncident(incidentId);
        requireNotClosed(incident);

        Analyst analyst = analystRepository.findById(analystId)
                .orElseThrow(() -> new NotFoundException("Analyst " + analystId + " not found"));
        if (!analyst.isActive()) {
            throw new InvalidStateException("Analyst " + analystId + " is inactive");
        }

        Long previousAssigneeId = assigneeIdOf(incident);
        incident.assignTo(analyst);

        Long newAssigneeId = analyst.getId();
        AfterCommit.run(() -> {
            if (previousAssigneeId != null) {
                workloadCache.decrement(previousAssigneeId);
            }
            workloadCache.increment(newAssigneeId);
        });
        return flushAndMap(incident);
    }

    public IncidentResponse start(Long incidentId) {
        Incident incident = findIncident(incidentId);
        if (incident.getStatus() != IncidentStatus.ASSIGNED) {
            throw new InvalidStateException(
                    "Only ASSIGNED incidents can be started (current: " + incident.getStatus() + ")");
        }
        incident.start();
        return flushAndMap(incident);
    }

    public IncidentResponse close(Long incidentId) {
        Incident incident = findIncident(incidentId);
        requireNotClosed(incident);
        incident.close();

        Long id = incident.getId();
        Long assigneeId = assigneeIdOf(incident);
        AfterCommit.run(() -> {
            triageQueue.remove(id);
            if (assigneeId != null) {
                workloadCache.decrement(assigneeId);
            }
        });
        return flushAndMap(incident);
    }

    /** Manual severity wins; otherwise ask the scorer if we have a confidence value. */
    private void applySeverity(Incident incident, CreateIncidentRequest request) {
        if (request.severity() != null) {
            incident.setManualSeverity(request.severity());
            return;
        }
        if (request.detectionConfidence() != null) {
            scoringClient.score(request.detectionConfidence())
                    .ifPresent(result -> incident.applyScore(result.severity(), result.riskScore()));
        }
    }

    /** Least-loaded active analyst according to the workload cache, if any. */
    private Optional<Analyst> autoAssignTarget() {
        return workloadCache.leastLoadedAnalyst().flatMap(analystRepository::findById);
    }

    private static Long assigneeIdOf(Incident incident) {
        return incident.getAssignee() == null ? null : incident.getAssignee().getId();
    }

    private Incident findIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
    }

    private void requireNotClosed(Incident incident) {
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new InvalidStateException("Incident " + incident.getId() + " is already closed");
        }
    }

    // Push pending changes to the DB now so @PreUpdate sets updatedAt before we build the response.
    private IncidentResponse flushAndMap(Incident incident) {
        incidentRepository.flush();
        return IncidentResponse.from(incident);
    }
}