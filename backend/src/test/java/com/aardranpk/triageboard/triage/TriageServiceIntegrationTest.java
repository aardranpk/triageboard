package com.aardranpk.triageboard.triage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;
import com.aardranpk.triageboard.incident.Incident;
import com.aardranpk.triageboard.incident.IncidentRepository;
import org.springframework.context.annotation.Import;
import com.aardranpk.triageboard.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TriageServiceIntegrationTest {

    @Autowired
    private TriageService triageService;

    @Autowired
    private TriageQueue triageQueue;

    @Autowired
    private WorkloadCache workloadCache;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AnalystRepository analystRepository;

    @Test
    void rebuildLoadsOpenIncidentsAndAnalystWorkloads() {
        Analyst analyst = analystRepository.save(
                new Analyst("Rebuild Tester", "rebuild+" + UUID.randomUUID() + "@example.com"));
        Incident open = new Incident("Open and assigned", null);
        open.assignTo(analyst);
        Incident closed = new Incident("Closed and assigned", null);
        closed.assignTo(analyst);
        closed.close();
        incidentRepository.saveAllAndFlush(List.of(open, closed));

        triageService.rebuild();

        assertThat(triageQueue.contains(open.getId())).isTrue();
        assertThat(triageQueue.contains(closed.getId())).isFalse();
        assertThat(workloadCache.loadOf(analyst.getId())).isEqualTo(1);
    }

    /** The test rows are rolled back, so rebuild again from the real committed data. */
    @AfterTransaction
    void restoreRealState() {
        triageService.rebuild();
    }
}