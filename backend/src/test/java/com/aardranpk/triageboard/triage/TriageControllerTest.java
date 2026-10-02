package com.aardranpk.triageboard.triage;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aardranpk.triageboard.incident.Severity;

@WebMvcTest(TriageController.class)
class TriageControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageService triageService;

    @Test
    void queueReturnsEntriesInPriorityOrder() throws Exception {
        when(triageService.queue()).thenReturn(List.of(
                new TriageEntry(5L, "Ransomware beacon", Severity.CRITICAL, T0),
                new TriageEntry(2L, "Port scan", Severity.HIGH, T0)));

        mockMvc.perform(get("/api/triage/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].incidentId").value(5))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[1].incidentId").value(2));
    }

    @Test
    void nextReturns204WhenQueueEmpty() throws Exception {
        when(triageService.next()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/triage/next"))
                .andExpect(status().isNoContent());
    }
}