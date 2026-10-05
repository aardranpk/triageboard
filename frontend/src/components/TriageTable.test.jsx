import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TriageTable from './TriageTable';

const rows = [
  {
    id: 4, rank: 1, title: 'Unusual admin login', severity: 'CRITICAL',
    status: 'ASSIGNED', assigneeName: 'Sam Rivera', createdAt: '2026-10-02T00:09:09Z',
  },
  {
    id: 1, rank: 2, title: 'Phishing email reported', severity: null,
    status: 'OPEN', assigneeName: null, createdAt: '2026-10-02T00:09:09Z',
  },
];

describe('TriageTable', () => {
  it('renders rows in the given priority order', () => {
    render(<TriageTable rows={rows} />);

    const bodyRows = screen.getAllByRole('row').slice(1); // skip the header row
    expect(within(bodyRows[0]).getByText('Unusual admin login')).toBeInTheDocument();
    expect(within(bodyRows[1]).getByText('Phishing email reported')).toBeInTheDocument();
  });

  it('shows placeholders for unscored and unassigned incidents', () => {
    render(<TriageTable rows={rows} />);

    expect(screen.getByText('Unscored')).toBeInTheDocument();
    expect(screen.getByText('Unassigned')).toBeInTheDocument();
  });

  it('calls onSelect with the incident id when a title is clicked', async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<TriageTable rows={rows} onSelect={onSelect} />);

    await user.click(screen.getByRole('button', { name: 'Phishing email reported' }));

    expect(onSelect).toHaveBeenCalledWith(1);
  });

  it('shows the empty message when there are no rows', () => {
    render(<TriageTable rows={[]} emptyMessage="No incidents match these filters." />);

    expect(screen.getByText('No incidents match these filters.')).toBeInTheDocument();
  });
});