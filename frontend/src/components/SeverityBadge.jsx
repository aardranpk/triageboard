export default function SeverityBadge({ severity }) {
  const level = severity ? severity.toLowerCase() : 'unscored';
  const label = severity ? severity.charAt(0) + severity.slice(1).toLowerCase() : 'Unscored';
  return <span className={`badge badge-${level}`}>{label}</span>;
}