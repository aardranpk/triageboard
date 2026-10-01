package com.aardranpk.triageboard.incident;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aardranpk.triageboard.common.InvalidStateException;
import com.aardranpk.triageboard.common.NotFoundException;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    private static IncidentResponse sample(long id, IncidentStatus status) {
        Instant now = Instant.parse("2026-09-30T12:00:00Z");
        return new IncidentResponse(id, "Port scan detected", null, status,
                Severity.HIGH, null, null, now, now, null);
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(incidentService.create(any(CreateIncidentRequest.class)))
                .thenReturn(sample(1L, IncidentStatus.OPEN));

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Port scan detected", "severity": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/incidents/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists());

        verifyNoInteractions(incidentService);
    }

    @Test
    void getReturns404WhenIncidentMissing() throws Exception {
        when(incidentService.getById(99L))
                .thenThrow(new NotFoundException("Incident 99 not found"));

        mockMvc.perform(get("/api/incidents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Incident 99 not found"));
    }

    @Test
    void closeReturns409WhenAlreadyClosed() throws Exception {
        when(incidentService.close(1L))
                .thenThrow(new InvalidStateException("Incident 1 is already closed"));

        mockMvc.perform(post("/api/incidents/1/close"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Incident 1 is already closed"));
    }

    @Test
    void listPassesStatusFilterToService() throws Exception {
        when(incidentService.list(IncidentStatus.OPEN))
                .thenReturn(List.of(sample(1L, IncidentStatus.OPEN), sample(2L, IncidentStatus.OPEN)));

        mockMvc.perform(get("/api/incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void assignRequiresAnalystId() throws Exception {
        mockMvc.perform(post("/api/incidents/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.analystId").exists());

        verifyNoInteractions(incidentService);
    }
}