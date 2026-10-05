import { useState } from 'react';
import SeverityBadge from './SeverityBadge';
import { formatStatus, timeAgo } from '../utils/format';

const SOURCE_LABELS = { SCORER: 'Model', MANUAL: 'Manual' };

export default function IncidentDetail({
  incident,
  analysts,
  busy,
  error,
  onAssign,
  onStart,
  onClose,
  onDismiss,
}) {
  const [analystId, setAnalystId] = useState('');
  const activeAnalysts = analysts.filter((a) => a.active);
  const canStart = incident.status === 'ASSIGNED';

  return (
    <aside className="detail" aria-label="Incident details">
      <div className="detail-header">
        <h2>#{incident.id} {incident.title}</h2>
        <button type="button" className="link-button" onClick={onDismiss} aria-label="Hide details">
          ✕
        </button>
      </div>

      <dl className="detail-fields">
        <dt>Severity</dt>
        <dd><SeverityBadge severity={incident.severity} /></dd>
        <dt>Severity source</dt>
        <dd>{SOURCE_LABELS[incident.severitySource] ?? '—'}</dd>
        <dt>Risk score</dt>
        <dd>{incident.riskScore != null ? `${incident.riskScore} / 100` : '—'}</dd>
        <dt>Detection confidence</dt>
        <dd>
          {incident.detectionConfidence != null
            ? `${Math.round(incident.detectionConfidence * 100)}%`
            : '—'}
        </dd>
        <dt>Status</dt>
        <dd>{formatStatus(incident.status)}</dd>
        <dt>Assignee</dt>
        <dd>{incident.assigneeName ?? 'Unassigned'}</dd>
        <dt>Created</dt>
        <dd>{new Date(incident.createdAt).toLocaleString()} ({timeAgo(incident.createdAt)})</dd>
        <dt>Updated</dt>
        <dd>{timeAgo(incident.updatedAt)}</dd>
      </dl>

      {incident.description && <p className="detail-description">{incident.description}</p>}

      <div className="detail-actions">
        <label className="field">
          <span>Assign to</span>
          <select value={analystId} onChange={(e) => setAnalystId(e.target.value)} disabled={busy}>
            <option value="">Choose analyst…</option>
            {activeAnalysts.map((a) => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>
        </label>
        <button type="button" onClick={() => onAssign(Number(analystId))} disabled={busy || !analystId}>
          Assign
        </button>
        <button type="button" onClick={onStart} disabled={busy || !canStart}>
          Start work
        </button>
        <button type="button" className="danger" onClick={onClose} disabled={busy}>
          Close incident
        </button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
    </aside>
  );
}