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