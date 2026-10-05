package com.aardranpk.triageboard.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.aardranpk.triageboard.incident.Severity;

class ScoringClientTest {

    private MockRestServiceServer server;
    private ScoringClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://scorer.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ScoringClient(builder.build());
    }

    @Test
    void mapsScorerResponseToSeverityAndRiskScore() {
        server.expect(requestTo("http://scorer.test/score"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.probability").value(0.95))
                .andRespond(withSuccess("""
                        {"risk_score": 95, "severity": "Critical", "model_probability": 0.95}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.score(0.95)).contains(new ScoreResult(Severity.CRITICAL, 95));
        server.verify();
    }

    @Test
    void returnsEmptyWhenScorerFails() {
        server.expect(requestTo("http://scorer.test/score"))
                .andRespond(withServerError());

        assertThat(client.score(0.5)).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownSeverityLabel() {
        server.expect(requestTo("http://scorer.test/score"))
                .andRespond(withSuccess("""
                        {"risk_score": 50, "severity": "Severe", "model_probability": 0.5}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.score(0.5)).isEmpty();
    }
}