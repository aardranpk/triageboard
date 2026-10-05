# TriageBoard

![CI](https://github.com/aardranpk/triageboard/actions/workflows/ci.yml/badge.svg)



Full-stack incident tracker: Spring Boot + PostgreSQL + React, with ML-based severity scoring.

## Data structures and complexity

TriageBoard keeps two in-memory structures alongside PostgreSQL. They are a deliberate
design choice for this project; a SQL `ORDER BY` could produce the same ordering on demand.

### Triage queue — `PriorityQueue<TriageEntry>` + `HashMap<Long, TriageEntry>`

Ordering: severity (CRITICAL → LOW, unscored last), then oldest first, then lowest id.

| Operation | Complexity | Notes |
|---|---|---|
| Add incident (`upsert`) | O(log n) | O(n) when replacing an existing entry |
| Highest priority (`peek`) | O(1) | |
| Remove incident (`remove`) | O(n) | `PriorityQueue.remove(Object)` is a linear search |
| Lookup by id (`contains`) | O(1) average | via the `HashMap` index |
| Full ordered list (`snapshot`) | O(n log n) | heap iteration order is not sorted, so a copy is sorted |

### Workload cache — `HashMap<Long, Integer>` (analyst id → open incidents)

| Operation | Complexity |
|---|---|
| Register / remove analyst | O(1) average |
| Increment / decrement load | O(1) average |
| Auto-assign (`leastLoadedAnalyst`) | O(a), a = active analysts |

A min-heap of analysts would make the auto-assign lookup O(1), but every load change would
then cost O(a) to re-position. With a small analyst pool, a linear scan is simpler.

### Limitations

- State lives in one application instance and is rebuilt from the database on startup;
  running multiple API instances would need a shared store (e.g., Redis) instead.
- All operations are `synchronized` — correct and simple, but a single lock per structure.
- Auto-assign runs when an incident is created. Queue and workload updates are applied
    only after the database transaction commits, so a rolled-back request never leaves the
    in-memory state out of sync.
- Two incidents created at the same instant may both go to the same analyst (load is
  incremented after commit); the result is a slight imbalance, never an error.

## Severity scoring

When an incident is created with a `detectionConfidence` (0–1) and no manual severity,
TriageBoard calls the [intrusion-risk-scoring-ai](https://github.com/aardranpk/intrusion-risk-scoring-ai)
FastAPI service (`POST /score`) and stores the returned severity and risk score.

- A manually supplied severity always wins; the scorer is not called.
- If the scorer is unreachable, slow (2 s timeout), or returns an unexpected response,
  the incident is still created, unscored, and a warning is logged.
- Trade-off: the scorer call happens inside the database transaction. At higher volume,
  scoring would move before the transaction or run asynchronously.