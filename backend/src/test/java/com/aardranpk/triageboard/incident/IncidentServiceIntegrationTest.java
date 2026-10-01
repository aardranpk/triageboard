package com.aardranpk.triageboard.incident;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IncidentServiceIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void closeResponseIncludesFreshUpdatedAt() {
        Incident saved = incidentRepository.saveAndFlush(new Incident("Timestamp check", null));

        IncidentResponse response = incidentService.close(saved.getId());

        assertThat(response.updatedAt()).isAfterOrEqualTo(response.closedAt());
    }
}