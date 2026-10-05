package com.aardranpk.triageboard.incident;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.aardranpk.triageboard.analyst.Analyst;

import jakarta.persistence.*;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_source", length = 10)
    private SeveritySource severitySource;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "detection_confidence")
    private Double detectionConfidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Analyst assignee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Incident() {
        // required by JPA
    }

    public Incident(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        Instant timestamp = now();
        createdAt = timestamp;
        updatedAt = timestamp;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = now();
    }

    public void assignTo(Analyst analyst) {
        this.assignee = analyst;
        this.status = IncidentStatus.ASSIGNED;
    }

    public void start() {
        this.status = IncidentStatus.IN_PROGRESS;
    }

    public void close() {
        this.status = IncidentStatus.CLOSED;
        this.closedAt = now();
    }

    /** Severity chosen by a person; any model score is not used. */
    public void setManualSeverity(Severity severity) {
        this.severity = severity;
        this.severitySource = SeveritySource.MANUAL;
    }

    /** Severity and risk score returned by the scoring service. */
    public void applyScore(Severity severity, int riskScore) {
        this.severity = severity;
        this.riskScore = riskScore;
        this.severitySource = SeveritySource.SCORER;
    }

    public void setDetectionConfidence(Double detectionConfidence) {
        this.detectionConfidence = detectionConfidence;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentStatus getStatus() { return status; }
    public Severity getSeverity() { return severity; }
    public SeveritySource getSeveritySource() { return severitySource; }
    public Integer getRiskScore() { return riskScore; }
    public Double getDetectionConfidence() { return detectionConfidence; }
    public Analyst getAssignee() { return assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }

    // Postgres stores microseconds; truncating keeps Java and DB values identical.
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}