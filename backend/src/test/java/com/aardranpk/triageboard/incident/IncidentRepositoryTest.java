package com.aardranpk.triageboard.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;

@SpringBootTest
@Transactional
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AnalystRepository analystRepository;

    @Test
    void savesIncidentWithTimestampsAndAssignee() {
        Analyst analyst = analystRepository.save(new Analyst("Alex Chen", "alex.chen@example.com"));
        Incident incident = new Incident("Suspicious login burst", "50 failed logins in 2 minutes");
        incident.assignTo(analyst);

        Incident saved = incidentRepository.saveAndFlush(incident);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getAssignee().getId()).isEqualTo(analyst.getId());
    }

    @Test
    void findByStatusNotExcludesClosedIncidents() {
        Incident open = new Incident("Open incident", null);
        Incident closed = new Incident("Closed incident", null);
        closed.close();
        incidentRepository.saveAllAndFlush(List.of(open, closed));

        List<Incident> active = incidentRepository.findByStatusNot(IncidentStatus.CLOSED);

        assertThat(active)
                .extracting(Incident::getTitle)
                .contains("Open incident")
                .doesNotContain("Closed incident");
    }
}