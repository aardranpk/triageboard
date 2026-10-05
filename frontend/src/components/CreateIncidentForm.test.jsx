import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CreateIncidentForm from './CreateIncidentForm';
import { api } from '../api';

vi.mock('../api', () => ({
  api: { createIncident: vi.fn() },
}));

describe('CreateIncidentForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends nulls for empty optional fields and reports success', async () => {
    api.createIncident.mockResolvedValue({ id: 1 });
    const onCreated = vi.fn();
    const user = userEvent.setup();
    render(<CreateIncidentForm onCreated={onCreated} />);

    await user.type(screen.getByRole('textbox', { name: 'Title' }), 'Port scan');
    await user.click(screen.getByRole('button', { name: 'Create incident' }));

    expect(api.createIncident).toHaveBeenCalledWith({
      title: 'Port scan',
      description: null,
      severity: null,
    });
    await waitFor(() => expect(onCreated).toHaveBeenCalled());
  });

  it('shows backend field errors next to the field', async () => {
    const validationError = Object.assign(new Error('One or more fields are invalid'), {
      status: 400,
      fieldErrors: { title: 'must not be blank' },
    });
    api.createIncident.mockRejectedValue(validationError);
    const onCreated = vi.fn();
    const user = userEvent.setup();
    render(<CreateIncidentForm onCreated={onCreated} />);

    await user.type(screen.getByRole('textbox', { name: 'Title' }), '   ');
    await user.click(screen.getByRole('button', { name: 'Create incident' }));

    expect(await screen.findByText('must not be blank')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('One or more fields are invalid');
    expect(onCreated).not.toHaveBeenCalled();
  });
});