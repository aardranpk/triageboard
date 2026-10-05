import { describe, expect, it } from 'vitest';
import { formatStatus, timeAgo } from './format';

const NOW = Date.parse('2026-10-05T12:00:00Z');

describe('timeAgo', () => {
  it('uses seconds and minutes for recent times', () => {
    expect(timeAgo('2026-10-05T11:59:30Z', NOW)).toBe('30s ago');
    expect(timeAgo('2026-10-05T11:45:00Z', NOW)).toBe('15m ago');
  });

  it('uses hours and days for older times', () => {
    expect(timeAgo('2026-10-05T09:00:00Z', NOW)).toBe('3h ago');
    expect(timeAgo('2026-10-03T12:00:00Z', NOW)).toBe('2d ago');
  });

  it('never reports a negative age', () => {
    expect(timeAgo('2026-10-05T12:00:05Z', NOW)).toBe('0s ago');
  });
});

describe('formatStatus', () => {
  it('turns enum names into readable labels', () => {
    expect(formatStatus('IN_PROGRESS')).toBe('In progress');
    expect(formatStatus('CRITICAL')).toBe('Critical');
    expect(formatStatus(null)).toBe('');
  });
});