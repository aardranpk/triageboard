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

import com.aardranpk.triageboard.analyst.Analyst;
import com.aardranpk.triageboard.analyst.AnalystRepository;
import com.aardranpk.triageboard.common.InvalidStateException;
import com.aardranpk.triageboard.common.NotFoundException;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AnalystRepository analystRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void createSavesOpenIncidentWithRequestedSeverity() {
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponse response = incidentService.create(
                new CreateIncidentRequest("Port scan detected", "Source 10.0.0.5", Severity.HIGH));

        assertThat(response.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.severity()).isEqualTo(Severity.HIGH);
        assertThat(response.title()).isEqualTo("Port scan detected");
        verify(incidentRepository).save(any(Incident.class));
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
    void closeRejectsAlreadyClosedIncident() {
        Incident incident = new Incident("Done", null);
        incident.close();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> incidentService.close(1L))
                .isInstanceOf(InvalidStateException.class);
    }
}