package com.aardranpk.triageboard.triage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;
import com.aardranpk.triageboard.incident.Incident;
import com.aardranpk.triageboard.incident.IncidentRepository;
import com.aardranpk.triageboard.incident.IncidentStatus;

@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final IncidentRepository incidentRepository;
    private final AnalystRepository analystRepository;
    private final TriageQueue triageQueue;
    private final WorkloadCache workloadCache;

    public TriageService(IncidentRepository incidentRepository,
                         AnalystRepository analystRepository,
                         TriageQueue triageQueue,
                         WorkloadCache workloadCache) {
        this.incidentRepository = incidentRepository;
        this.analystRepository = analystRepository;
        this.triageQueue = triageQueue;
        this.workloadCache = workloadCache;
    }

    /** Rebuilds in-memory triage state from the database. Runs once at startup. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void rebuild() {
        List<Incident> active = incidentRepository.findByStatusNot(IncidentStatus.CLOSED);

        triageQueue.clear();
        active.forEach(incident -> triageQueue.upsert(TriageEntry.from(incident)));

        Map<Long, Integer> counts = new HashMap<>();
        for (Analyst analyst : analystRepository.findByActiveTrue()) {
            counts.put(analyst.getId(), 0);
        }
        for (Incident incident : active) {
            if (incident.getAssignee() != null) {
                counts.computeIfPresent(incident.getAssignee().getId(), (id, count) -> count + 1);
            }
        }
        workloadCache.replaceAll(counts);

        log.info("Triage state rebuilt: {} queued incidents, {} active analysts",
                triageQueue.size(), counts.size());
    }

    public List<TriageEntry> queue() {
        return triageQueue.snapshot();
    }

    public Optional<TriageEntry> next() {
        return triageQueue.peek();
    }

    public Map<Long, Integer> workload() {
        return workloadCache.snapshot();
    }
}