export function timeAgo(isoString, now = Date.now()) {
  const seconds = Math.max(0, Math.floor((now - new Date(isoString).getTime()) / 1000));
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

export function formatStatus(status) {
  if (!status) return '';
  const text = status.toLowerCase().replaceAll('_', ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}