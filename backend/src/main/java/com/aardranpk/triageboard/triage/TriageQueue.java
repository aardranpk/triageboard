package com.aardranpk.triageboard.triage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

import com.aardranpk.triageboard.incident.Severity;

/**
 * In-memory triage queue: highest severity first, then oldest, then lowest id.
 * Unscored incidents (null severity) sort last.
 */
public class TriageQueue {

    public static final Comparator<TriageEntry> PRIORITY =
            Comparator.comparing(TriageEntry::severity,
                            Comparator.nullsLast(Comparator.<Severity>reverseOrder()))
                    .thenComparing(TriageEntry::createdAt)
                    .thenComparing(TriageEntry::incidentId);

    private final PriorityQueue<TriageEntry> queue = new PriorityQueue<>(PRIORITY);
    private final Map<Long, TriageEntry> byId = new HashMap<>();

    /** Adds an entry, replacing any existing entry for the same incident. */
    public synchronized void upsert(TriageEntry entry) {
        TriageEntry previous = byId.put(entry.incidentId(), entry);
        if (previous != null) {
            queue.remove(previous);
        }
        queue.offer(entry);
    }

    public synchronized boolean remove(Long incidentId) {
        TriageEntry entry = byId.remove(incidentId);
        return entry != null && queue.remove(entry);
    }

    public synchronized Optional<TriageEntry> peek() {
        return Optional.ofNullable(queue.peek());
    }

    public synchronized boolean contains(Long incidentId) {
        return byId.containsKey(incidentId);
    }

    public synchronized int size() {
        return queue.size();
    }

    /** All entries in priority order. Iterating a PriorityQueue directly is NOT sorted. */
    public synchronized List<TriageEntry> snapshot() {
        List<TriageEntry> sorted = new ArrayList<>(queue);
        sorted.sort(PRIORITY);
        return sorted;
    }

    public synchronized void clear() {
        queue.clear();
        byId.clear();
    }
}