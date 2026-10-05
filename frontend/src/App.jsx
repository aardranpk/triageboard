import { useEffect, useState } from 'react';
import { api } from './api';
import { fetchTriageRows, filterRows } from './triage';
import TriageTable from './components/TriageTable';
import Filters from './components/Filters';
import IncidentDetail from './components/IncidentDetail';
import CreateIncidentForm from './components/CreateIncidentForm';
import './App.css';

const NO_FILTERS = { severity: '', status: '', assignee: '' };

export default function App() {
  const [rows, setRows] = useState([]);
  const [analysts, setAnalysts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [filters, setFilters] = useState(NO_FILTERS);
  const [selectedId, setSelectedId] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([fetchTriageRows(), api.listAnalysts()])
      .then(([triageRows, analystList]) => {
        if (cancelled) return;
        setRows(triageRows);
        setAnalysts(analystList);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  function refresh() {
    setLoading(true);
    setError(null);
    setReloadKey((key) => key + 1);
  }

  async function runAction(action) {
    setBusy(true);
    setActionError(null);
    try {
      await action();
      refresh();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function selectIncident(id) {
    setSelectedId(id);
    setActionError(null);
  }

  const visibleRows = filterRows(rows, filters);
  const selected = rows.find((row) => row.id === selectedId) ?? null;
  const filtersActive = Object.values(filters).some(Boolean);

  return (
    <main className="app">
      <header className="app-header">
        <h1>TriageBoard</h1>
        <div className="header-actions">
          <button type="button" onClick={() => setShowCreate((open) => !open)}>
            {showCreate ? 'Cancel' : 'New incident'}
          </button>
          <button type="button" onClick={refresh} disabled={loading}>
            {loading ? 'Loading…' : 'Refresh'}
          </button>
        </div>
      </header>

      {showCreate && (
        <CreateIncidentForm
          onCreated={() => {
            setShowCreate(false);
            refresh();
          }}
        />
      )}

      {error && <p className="error" role="alert">{error}</p>}

      {!error && (
        <>
          <Filters filters={filters} analysts={analysts} onChange={setFilters} />

          <div className={selected ? 'layout layout-with-detail' : 'layout'}>
            <section>
              {loading && rows.length === 0 ? (
                <p className="empty">Loading…</p>
              ) : (
                <TriageTable
                  rows={visibleRows}
                  selectedId={selectedId}
                  onSelect={selectIncident}
                  emptyMessage={filtersActive ? 'No incidents match these filters.' : 'No open incidents.'}
                />
              )}
            </section>

            {selected && (
              <IncidentDetail
                key={selected.id}
                incident={selected}
                analysts={analysts}
                busy={busy}
                error={actionError}
                onAssign={(analystId) => runAction(() => api.assignIncident(selected.id, analystId))}
                onStart={() => runAction(() => api.startIncident(selected.id))}
                onClose={() => runAction(() => api.closeIncident(selected.id))}
                onDismiss={() => setSelectedId(null)}
              />
            )}
          </div>
        </>
      )}
    </main>
  );
}