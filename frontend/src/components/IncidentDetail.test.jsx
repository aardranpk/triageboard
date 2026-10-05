import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IncidentDetail from './IncidentDetail';

const analysts = [
  { id: 1, name: 'Alex Chen', email: 'alex@example.com', active: true },
  { id: 2, name: 'Sam Rivera', email: 'sam@example.com', active: true },
  { id: 3, name: 'Former Analyst', email: 'former@example.com', active: false },
];

function makeIncident(overrides = {}) {
  return {
    id: 7,
    title: 'Port scan from 10.0.0.5',
    description: null,
    severity: 'HIGH',
    status: 'OPEN',
    assigneeName: null,
    createdAt: '2026-10-02T00:09:09Z',
    updatedAt: '2026-10-02T00:09:09Z',
    ...overrides,
  };
}

function renderDetail(incident = makeIncident()) {
  const handlers = { onAssign: vi.fn(), onStart: vi.fn(), onClose: vi.fn(), onDismiss: vi.fn() };
  render(
    <IncidentDetail incident={incident} analysts={analysts} busy={false} error={null} {...handlers} />,
  );
  return handlers;
}

describe('IncidentDetail', () => {
  it('disables Start work when the incident is not assigned', () => {
    renderDetail(makeIncident({ status: 'OPEN' }));

    expect(screen.getByRole('button', { name: 'Start work' })).toBeDisabled();
  });

  it('enables Start work for assigned incidents', () => {
    renderDetail(makeIncident({ status: 'ASSIGNED', assigneeName: 'Alex Chen' }));

    expect(screen.getByRole('button', { name: 'Start work' })).toBeEnabled();
  });

  it('lists only active analysts', () => {
    renderDetail();

    expect(screen.getByRole('option', { name: 'Sam Rivera' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Former Analyst' })).not.toBeInTheDocument();
  });

  it('assigns the chosen analyst, sending the id as a number', async () => {
    const handlers = renderDetail();
    const user = userEvent.setup();
    const assignButton = screen.getByRole('button', { name: 'Assign' });

    expect(assignButton).toBeDisabled();
    await user.selectOptions(screen.getByRole('combobox'), '2');
    await user.click(assignButton);

    expect(handlers.onAssign).toHaveBeenCalledWith(2);
  });
  it('shows model scoring details', () => {
    renderDetail(makeIncident({
      severity: 'CRITICAL',
      severitySource: 'SCORER',
      riskScore: 95,
      detectionConfidence: 0.95,
    }));

    expect(screen.getByText('Model')).toBeInTheDocument();
    expect(screen.getByText('95 / 100')).toBeInTheDocument();
    expect(screen.getByText('95%')).toBeInTheDocument();
  });
});