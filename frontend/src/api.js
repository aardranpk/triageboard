const UNREACHABLE = "Can't reach the TriageBoard API. Is the backend running?";

async function request(path, options = {}) {
  const { headers, ...rest } = options;

  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...rest,
      headers: { 'Content-Type': 'application/json', ...headers },
    });
  } catch {
    throw new Error(UNREACHABLE);
  }

  if (response.status === 204) {
    return null;
  }

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    const fallback = [502, 503, 504].includes(response.status)
      ? UNREACHABLE
      : `Request failed with status ${response.status}`;
    const error = new Error(body?.detail ?? fallback);
    error.status = response.status;
    error.fieldErrors = body?.errors ?? null;
    throw error;
  }
  return body;
}

export const api = {
  getTriageQueue: () => request('/triage/queue'),
  getWorkload: () => request('/triage/workload'),
  listIncidents: (status) => request(status ? `/incidents?status=${status}` : '/incidents'),
  getIncident: (id) => request(`/incidents/${id}`),
  createIncident: (data) =>
    request('/incidents', { method: 'POST', body: JSON.stringify(data) }),
  assignIncident: (id, analystId) =>
    request(`/incidents/${id}/assign`, { method: 'POST', body: JSON.stringify({ analystId }) }),
  startIncident: (id) => request(`/incidents/${id}/start`, { method: 'POST' }),
  closeIncident: (id) => request(`/incidents/${id}/close`, { method: 'POST' }),
  listAnalysts: () => request('/analysts'),
};