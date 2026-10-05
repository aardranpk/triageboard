import { describe, expect, it } from 'vitest';
import { filterRows, joinQueueWithIncidents } from './triage';

describe('joinQueueWithIncidents', () => {
  it('keeps the backend queue order and assigns ranks', () => {
    const queue = [{ incidentId: 4 }, { incidentId: 1 }];
    const incidents = [
      { id: 1, title: 'Low priority' },
      { id: 4, title: 'Critical' },
    ];

    expect(joinQueueWithIncidents(queue, incidents)).toEqual([
      { rank: 1, id: 4, title: 'Critical' },
      { rank: 2, id: 1, title: 'Low priority' },
    ]);
  });

  it('drops queue entries that have no matching incident', () => {
    expect(joinQueueWithIncidents([{ incidentId: 99 }], [])).toEqual([]);
  });
});

describe('filterRows', () => {
  const rows = [
    { id: 1, severity: 'CRITICAL', status: 'ASSIGNED', assigneeId: 10 },
    { id: 2, severity: null, status: 'OPEN', assigneeId: null },
    { id: 3, severity: 'LOW', status: 'IN_PROGRESS', assigneeId: 20 },
  ];
  const noFilters = { severity: '', status: '', assignee: '' };
  const ids = (result) => result.map((row) => row.id);

  it('returns every row when no filters are set', () => {
    expect(ids(filterRows(rows, noFilters))).toEqual([1, 2, 3]);
  });

  it('filters by severity, including unscored', () => {
    expect(ids(filterRows(rows, { ...noFilters, severity: 'CRITICAL' }))).toEqual([1]);
    expect(ids(filterRows(rows, { ...noFilters, severity: 'UNSCORED' }))).toEqual([2]);
  });

  it('filters by assignee, including unassigned', () => {
    expect(ids(filterRows(rows, { ...noFilters, assignee: '20' }))).toEqual([3]);
    expect(ids(filterRows(rows, { ...noFilters, assignee: 'UNASSIGNED' }))).toEqual([2]);
  });

  it('combines filters', () => {
    expect(ids(filterRows(rows, { ...noFilters, severity: 'LOW', status: 'IN_PROGRESS' }))).toEqual([3]);
    expect(ids(filterRows(rows, { ...noFilters, severity: 'LOW', status: 'OPEN' }))).toEqual([]);
  });
});