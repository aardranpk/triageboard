import { useState } from 'react';
import { api } from '../api';
import { SEVERITIES } from '../constants';
import { formatStatus } from '../utils/format';

export default function CreateIncidentForm({ onCreated }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [severity, setSeverity] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      await api.createIncident({
        title,
        description: description || null,
        severity: severity || null,
      });
      onCreated();
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="create-form" onSubmit={handleSubmit}>
      <h2>New incident</h2>

      <label className="field">
        <span>Title</span>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          maxLength={200}
          disabled={submitting}
        />
        {fieldErrors.title && <small className="field-error">{fieldErrors.title}</small>}
      </label>

      <label className="field">
        <span>Description</span>
        <textarea
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          maxLength={5000}
          disabled={submitting}
        />
        {fieldErrors.description && (
          <small className="field-error">{fieldErrors.description}</small>
        )}
      </label>

      <label className="field">
        <span>Severity</span>
        <select value={severity} onChange={(e) => setSeverity(e.target.value)} disabled={submitting}>
          <option value="">Unscored</option>
          {SEVERITIES.map((s) => (
            <option key={s} value={s}>{formatStatus(s)}</option>
          ))}
        </select>
      </label>

      {error && <p className="error" role="alert">{error}</p>}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Creating…' : 'Create incident'}
      </button>
    </form>
  );
}