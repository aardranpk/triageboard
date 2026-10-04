import { useEffect, useState } from 'react';
import { fetchTriageRows } from './triage';
import TriageTable from './components/TriageTable';
import './App.css';

export default function App() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    fetchTriageRows()
      .then((data) => {
        if (!cancelled) setRows(data);
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

  return (
    <main className="app">
      <header className="app-header">
        <h1>TriageBoard</h1>
        <button type="button" onClick={refresh} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </header>

      {error && <p className="error" role="alert">{error}</p>}
      {!error && !loading && <TriageTable rows={rows} />}
    </main>
  );
}