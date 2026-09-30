package com.aardranpk.triageboard.incident;

import java.time.Instant;

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
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void assignTo(Analyst analyst) {
        this.assignee = analyst;
        this.status = IncidentStatus.ASSIGNED;
    }

    public void close() {
        this.status = IncidentStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public void setSeverity(Severity severity) { this.severity = severity; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentStatus getStatus() { return status; }
    public Severity getSeverity() { return severity; }
    public Analyst getAssignee() { return assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }
}