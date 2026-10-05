package com.aardranpk.triageboard.scoring;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.aardranpk.triageboard.incident.Severity;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Calls the intrusion-risk-scoring-ai FastAPI service.
 * Never throws: any failure returns Optional.empty() so incident creation can continue.
 */
public class ScoringClient {

    private static final Logger log = LoggerFactory.getLogger(ScoringClient.class);

    private final RestClient restClient;

    public ScoringClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<ScoreResult> score(double probability) {
        try {
            ScoreResponse response = restClient.post()
                    .uri("/score")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ScoreRequest(probability))
                    .retrieve()
                    .body(ScoreResponse.class);

            if (response == null || response.severity() == null) {
                log.warn("Scoring service returned an empty response");
                return Optional.empty();
            }
            Severity severity = Severity.valueOf(response.severity().toUpperCase(Locale.ROOT));
            return Optional.of(new ScoreResult(severity, response.riskScore()));
        } catch (RestClientException | IllegalArgumentException ex) {
            log.warn("Scoring unavailable, incident will be unscored: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public record ScoreRequest(double probability) {
    }

    public record ScoreResponse(
            @JsonProperty("risk_score") int riskScore,
            String severity,
            @JsonProperty("model_probability") double modelProbability) {
    }
}