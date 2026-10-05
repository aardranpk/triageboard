package com.aardranpk.triageboard.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;
import com.aardranpk.triageboard.common.InvalidStateException;
import com.aardranpk.triageboard.common.NotFoundException;
import com.aardranpk.triageboard.scoring.ScoreResult;
import com.aardranpk.triageboard.scoring.ScoringClient;
import com.aardranpk.triageboard.triage.TriageEntry;
import com.aardranpk.triageboard.triage.TriageQueue;
import com.aardranpk.triageboard.triage.WorkloadCache;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AnalystRepository analystRepository;

    @Mock
    private TriageQueue triageQueue;

    @Mock
    private WorkloadCache workloadCache;

    @Mock
    private ScoringClient scoringClient;

    @InjectMocks
    private IncidentService incidentService;

    private void saveReturnsArgument() {
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createKeepsManualSeverityWithoutCallingScorer() {
        saveReturnsArgument();

        IncidentResponse response = incidentService.create(
                new CreateIncidentRequest("Port scan detected", "Source 10.0.0.5", Severity.HIGH, 0.95));

        assertThat(response.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.severity()).isEqualTo(Severity.HIGH);
        assertThat(response.severitySource()).isEqualTo(SeveritySource.MANUAL);
        assertThat(response.riskScore()).isNull();
        verifyNoInteractions(scoringClient);
        verify(triageQueue).upsert(any(TriageEntry.class));
    }

    @Test
    void createUsesScorerWhenOnlyConfidenceIsProvided() {
        saveReturnsArgument();
        when(scoringClient.score(0.95)).thenReturn(Optional.of(new ScoreResult(Severity.CRITICAL, 95)));

        IncidentResponse response = incidentService.create(
                new CreateIncidentRequest("Ransomware beacon", null, null, 0.95));

        assertThat(response.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(response.severitySource()).isEqualTo(SeveritySource.SCORER);
        assertThat(response.riskScore()).isEqualTo(95);
        assertThat(response.detectionConfidence()).isEqualTo(0.95);
    }

    @Test
    void createLeavesIncidentUnscoredWhenScorerUnavailable() {
        saveReturnsArgument();
        when(scoringClient.score(0.8)).thenReturn(Optional.empty());

        IncidentResponse response = incidentService.create(
                new CreateIncidentRequest("Suspicious DNS traffic", null, null, 0.8));

        assertThat(response.severity()).isNull();
        assertThat(response.severitySource()).isNull();
        assertThat(response.detectionConfidence()).isEqualTo(0.8);
        verify(triageQueue).upsert(any(TriageEntry.class));
    }

    @Test
    void createWithoutSeverityOrConfidenceSkipsScorer() {
        saveReturnsArgument();

        IncidentResponse response = incidentService.create(
                new CreateIncidentRequest("Manual report", null, null, null));

        assertThat(response.severity()).isNull();
        verifyNoInteractions(scoringClient);
    }

    @Test
    void createAutoAssignsLeastLoadedAnalyst() {
        Analyst analyst = new Analyst("Alex Chen", "alex.chen@example.com");
        ReflectionTestUtils.setField(analyst, "id", 7L);
        when(workloadCache.leastLoadedAnalyst()).thenReturn(Optional.of(7L));
        when(analystRepository.findById(7L)).thenReturn(Optional.of(analyst));
        saveReturnsArgument();

        IncidentResponse response = incidentService.create(
                new CreateIncidentRequest("Ransomware beacon", null, Severity.CRITICAL, null));

        assertThat(response.status()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(response.assigneeName()).isEqualTo("Alex Chen");
        verify(triageQueue).upsert(any(TriageEntry.class));
        verify(workloadCache).increment(7L);
    }

    @Test
    void getByIdThrowsWhenIncidentMissing() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void assignSetsAssigneeAndStatus() {
        Incident incident = new Incident("Malware alert", null);
        Analyst analyst = new Analyst("Alex Chen", "alex.chen@example.com");
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(analystRepository.findById(7L)).thenReturn(Optional.of(analyst));

        IncidentResponse response = incidentService.assign(1L, 7L);

        assertThat(response.status()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(response.assigneeName()).isEqualTo("Alex Chen");
    }

    @Test
    void assignRejectsClosedIncident() {
        Incident incident = new Incident("Old alert", null);
        incident.close();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.assign(1L, 7L))
                .isInstanceOf(InvalidStateException.class);
        verifyNoInteractions(analystRepository);
    }

    @Test
    void assignRejectsInactiveAnalyst() {
        Incident incident = new Incident("Phishing report", null);
        Analyst analyst = new Analyst("Sam Rivera", "sam.rivera@example.com");
        analyst.deactivate();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(analystRepository.findById(7L)).thenReturn(Optional.of(analyst));

        assertThatThrownBy(() -> incidentService.assign(1L, 7L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void startRequiresAssignedStatus() {
        Incident incident = new Incident("Unassigned alert", null);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.start(1L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("OPEN");
    }

    @Test
    void closeSetsStatusAndTimestamp() {
        Incident incident = new Incident("Resolved alert", null);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        IncidentResponse response = incidentService.close(1L);

        assertThat(response.status()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(response.closedAt()).isNotNull();
    }

    @Test
    void closeRemovesFromQueueAndReleasesAnalystLoad() {
        Analyst analyst = new Analyst("Alex Chen", "alex.chen@example.com");
        ReflectionTestUtils.setField(analyst, "id", 7L);
        Incident incident = new Incident("Assigned alert", null);
        ReflectionTestUtils.setField(incident, "id", 1L);
        incident.assignTo(analyst);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        incidentService.close(1L);

        verify(triageQueue).remove(1L);
        verify(workloadCache).decrement(7L);
    }

    @Test
    void closeRejectsAlreadyClosedIncident() {
        Incident incident = new Incident("Done", null);
        incident.close();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.close(1L))
                .isInstanceOf(InvalidStateException.class);
    }
}