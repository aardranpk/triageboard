package com.aardranpk.triageboard.triage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkloadCacheTest {

    @Test
    void leastLoadedPicksLowestOpenCount() {
        WorkloadCache cache = new WorkloadCache();
        cache.replaceAll(Map.of(1L, 3, 2L, 1, 3L, 2));

        assertThat(cache.leastLoadedAnalyst()).contains(2L);
    }

    @Test
    void tieGoesToLowestAnalystId() {
        WorkloadCache cache = new WorkloadCache();
        cache.replaceAll(Map.of(7L, 0, 3L, 0, 5L, 0));

        assertThat(cache.leastLoadedAnalyst()).contains(3L);
    }

    @Test
    void emptyCacheHasNoCandidate() {
        assertThat(new WorkloadCache().leastLoadedAnalyst()).isEmpty();
    }

    @Test
    void decrementNeverGoesBelowZero() {
        WorkloadCache cache = new WorkloadCache();
        cache.registerAnalyst(1L);

        cache.decrement(1L);

        assertThat(cache.loadOf(1L)).isZero();
    }

    @Test
    void incrementIgnoresUnregisteredAnalyst() {
        WorkloadCache cache = new WorkloadCache();

        cache.increment(42L);

        assertThat(cache.leastLoadedAnalyst()).isEmpty();
    }

    @Test
    void assignmentsShiftTheLeastLoadedAnalyst() {
        WorkloadCache cache = new WorkloadCache();
        cache.registerAnalyst(1L);
        cache.registerAnalyst(2L);

        cache.increment(1L);
        assertThat(cache.leastLoadedAnalyst()).contains(2L);

        cache.increment(2L);
        cache.increment(2L);
        assertThat(cache.leastLoadedAnalyst()).contains(1L);
    }
}