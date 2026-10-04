import SeverityBadge from './SeverityBadge';
import { formatStatus, timeAgo } from '../utils/format';

export default function TriageTable({ rows }) {
  if (rows.length === 0) {
    return <p className="empty">No open incidents.</p>;
  }

  return (
    <div className="table-wrap">
      <table className="triage-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Incident</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Assignee</th>
            <th>Age</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.rank}</td>
              <td>{row.title}</td>
              <td><SeverityBadge severity={row.severity} /></td>
              <td>{formatStatus(row.status)}</td>
              <td>{row.assigneeName ?? 'Unassigned'}</td>
              <td>{timeAgo(row.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}