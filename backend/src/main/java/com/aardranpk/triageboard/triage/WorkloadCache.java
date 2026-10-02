package com.aardranpk.triageboard.triage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Open (non-closed) incident count per active analyst.
 * Only registered (active) analysts are eligible for auto-assignment.
 */
@Component
public class WorkloadCache {

    private final Map<Long, Integer> openCounts = new HashMap<>();

    public synchronized void registerAnalyst(Long analystId) {
        openCounts.putIfAbsent(analystId, 0);
    }

    public synchronized void removeAnalyst(Long analystId) {
        openCounts.remove(analystId);
    }

    /** No-op for unregistered analysts, so inactive analysts never enter the pool. */
    public synchronized void increment(Long analystId) {
        openCounts.computeIfPresent(analystId, (id, count) -> count + 1);
    }

    public synchronized void decrement(Long analystId) {
        openCounts.computeIfPresent(analystId, (id, count) -> Math.max(0, count - 1));
    }

    public synchronized int loadOf(Long analystId) {
        return openCounts.getOrDefault(analystId, 0);
    }

    /** Auto-assign: lowest open count wins; ties go to the lowest analyst id. O(a). */
    public synchronized Optional<Long> leastLoadedAnalyst() {
        Long bestId = null;
        int bestLoad = Integer.MAX_VALUE;
        for (Map.Entry<Long, Integer> entry : openCounts.entrySet()) {
            Long id = entry.getKey();
            int load = entry.getValue();
            if (bestId == null || load < bestLoad || (load == bestLoad && id < bestId)) {
                bestId = id;
                bestLoad = load;
            }
        }
        return Optional.ofNullable(bestId);
    }

    public synchronized void replaceAll(Map<Long, Integer> counts) {
        openCounts.clear();
        openCounts.putAll(counts);
    }

    /** Immutable copy for read-only callers (e.g. the /api/triage/workload endpoint). */
    public synchronized Map<Long, Integer> snapshot() {
        return Map.copyOf(openCounts);
    }
}