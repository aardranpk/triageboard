import { api } from './api';

/** Combines the backend's priority order with full incident details. */
export function joinQueueWithIncidents(queue, incidents) {
  const byId = new Map(incidents.map((incident) => [incident.id, incident]));
  return queue
    .map((entry, index) => ({ rank: index + 1, ...byId.get(entry.incidentId) }))
    .filter((row) => row.id != null);
}

export async function fetchTriageRows() {
  const [queue, incidents] = await Promise.all([
    api.getTriageQueue(),
    api.listIncidents(),
  ]);
  return joinQueueWithIncidents(queue, incidents);
}

function matchesSeverity(row, severity) {
  if (!severity) return true;
  if (severity === 'UNSCORED') return row.severity == null;
  return row.severity === severity;
}

function matchesAssignee(row, assignee) {
  if (!assignee) return true;
  if (assignee === 'UNASSIGNED') return row.assigneeId == null;
  return String(row.assigneeId) === assignee;
}

/** Client-side filtering. An empty string means "no filter" for that field. */
export function filterRows(rows, filters) {
  return rows.filter(
    (row) =>
      matchesSeverity(row, filters.severity) &&
      (!filters.status || row.status === filters.status) &&
      matchesAssignee(row, filters.assignee),
  );
}