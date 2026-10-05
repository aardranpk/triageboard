import { ACTIVE_STATUSES, SEVERITIES } from '../constants';
import { formatStatus } from '../utils/format';

export default function Filters({ filters, analysts, onChange }) {
  function update(field, value) {
    onChange({ ...filters, [field]: value });
  }

  return (
    <div className="filters">
      <label className="field">
        <span>Severity</span>
        <select value={filters.severity} onChange={(e) => update('severity', e.target.value)}>
          <option value="">All</option>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>{formatStatus(s)}</option>
          ))}
          <option value="UNSCORED">Unscored</option>
        </select>
      </label>

      <label className="field">
        <span>Status</span>
        <select value={filters.status} onChange={(e) => update('status', e.target.value)}>
          <option value="">All</option>
          {ACTIVE_STATUSES.map((s) => (
            <option key={s} value={s}>{formatStatus(s)}</option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Assignee</span>
        <select value={filters.assignee} onChange={(e) => update('assignee', e.target.value)}>
          <option value="">All</option>
          <option value="UNASSIGNED">Unassigned</option>
          {analysts.map((a) => (
            <option key={a.id} value={String(a.id)}>{a.name}</option>
          ))}
        </select>
      </label>
    </div>
  );
}