package com.aardranpk.triageboard.incident;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;
import com.aardranpk.triageboard.common.InvalidStateException;
import com.aardranpk.triageboard.common.NotFoundException;

@Service
@Transactional
public class IncidentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final IncidentRepository incidentRepository;
    private final AnalystRepository analystRepository;

    public IncidentService(IncidentRepository incidentRepository,
                           AnalystRepository analystRepository) {
        this.incidentRepository = incidentRepository;
        this.analystRepository = analystRepository;
    }

    public IncidentResponse create(CreateIncidentRequest request) {
        Incident incident = new Incident(request.title(), request.description());
        incident.setSeverity(request.severity());
        return IncidentResponse.from(incidentRepository.save(incident));
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

        incident.assignTo(analyst);
        return IncidentResponse.from(incident);
    }

    public IncidentResponse start(Long incidentId) {
        Incident incident = findIncident(incidentId);
        if (incident.getStatus() != IncidentStatus.ASSIGNED) {
            throw new InvalidStateException(
                    "Only ASSIGNED incidents can be started (current: " + incident.getStatus() + ")");
        }
        incident.start();
        return IncidentResponse.from(incident);
    }

    public IncidentResponse close(Long incidentId) {
        Incident incident = findIncident(incidentId);
        requireNotClosed(incident);
        incident.close();
        return IncidentResponse.from(incident);
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
}