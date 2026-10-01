package com.aardranpk.triageboard.triage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.aardranpk.triageboard.incident.Severity;

class TriageQueueTest {

    private static final Instant T0 = Instant.parse("2026-10-01T09:00:00Z");

    private static TriageEntry entry(long id, Severity severity, int minutesAfterT0) {
        return new TriageEntry(id, "Incident " + id, severity, T0.plusSeconds(minutesAfterT0 * 60L));
    }

    @Test
    void ordersBySeverityThenAgeWithUnscoredLast() {
        TriageQueue queue = new TriageQueue();
        queue.upsert(entry(1, Severity.LOW, 0));
        queue.upsert(entry(2, null, 0));
        queue.upsert(entry(3, Severity.CRITICAL, 10));
        queue.upsert(entry(4, Severity.HIGH, 0));
        queue.upsert(entry(5, Severity.CRITICAL, 5));

        assertThat(queue.snapshot())
                .extracting(TriageEntry::incidentId)
                .containsExactly(5L, 3L, 4L, 1L, 2L);
    }

    @Test
    void peekReturnsHighestPriorityWithoutRemoving() {
        TriageQueue queue = new TriageQueue();
        queue.upsert(entry(1, Severity.MEDIUM, 0));
        queue.upsert(entry(2, Severity.CRITICAL, 30));

        assertThat(queue.peek()).map(TriageEntry::incidentId).contains(2L);
        assertThat(queue.size()).isEqualTo(2);
    }

    @Test
    void tieOnSeverityAndAgeBreaksOnLowestId() {
        TriageQueue queue = new TriageQueue();
        queue.upsert(entry(9, Severity.HIGH, 0));
        queue.upsert(entry(4, Severity.HIGH, 0));

        assertThat(queue.peek()).map(TriageEntry::incidentId).contains(4L);
    }

    @Test
    void removeDropsEntryAndReportsMissing() {
        TriageQueue queue = new TriageQueue();
        queue.upsert(entry(1, Severity.HIGH, 0));

        assertThat(queue.remove(1L)).isTrue();
        assertThat(queue.remove(1L)).isFalse();
        assertThat(queue.size()).isZero();
        assertThat(queue.peek()).isEmpty();
    }

    @Test
    void upsertReplacesExistingEntryForSameIncident() {
        TriageQueue queue = new TriageQueue();
        queue.upsert(entry(1, Severity.LOW, 0));
        queue.upsert(entry(2, Severity.MEDIUM, 0));

        queue.upsert(entry(1, Severity.CRITICAL, 0));

        assertThat(queue.size()).isEqualTo(2);
        assertThat(queue.peek()).map(TriageEntry::incidentId).contains(1L);
    }
}