import { useState } from 'react';
import { sorteosApi, Sorteo } from '../../services/api';

export function BuscadorSorteos() {
  const [numero, setNumero] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [resultados, setResultados] = useState<Sorteo[]>([]);
  const [loading, setLoading] = useState(false);

  const buscarPorNumero = async () => {
    if (!numero) return;
    setLoading(true);
    try {
      const res = await sorteosApi.porNumero(parseInt(numero));
      setResultados([res.data]);
    } catch {
      setResultados([]);
    } finally {
      setLoading(false);
    }
  };

  const buscarPorRango = async () => {
    if (!desde || !hasta) return;
    setLoading(true);
    try {
      const res = await sorteosApi.porRango(desde, hasta);
      setResultados(res.data);
    } catch {
      setResultados([]);
    } finally {
      setLoading(false);
    }
  };

  const listarTodos = async () => {
    setLoading(true);
    try {
      const res = await sorteosApi.listar();
      setResultados(res.data);
    } catch {
      setResultados([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem' }}>
        Buscador de Sorteos
      </h1>

      {/* Search by number */}
      <div className="card" style={{ marginBottom: '1rem' }}>
        <h2 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Por número de sorteo</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input
            type="number"
            value={numero}
            onChange={(e) => setNumero(e.target.value)}
            placeholder="Ej: 3396"
            style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #dee2e6', flex: 1 }}
          />
          <button className="btn btn-primary" onClick={buscarPorNumero} disabled={loading}>
            Buscar
          </button>
        </div>
      </div>

      {/* Search by date range */}
      <div className="card" style={{ marginBottom: '1rem' }}>
        <h2 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Por rango de fechas</h2>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #dee2e6' }} />
          <span>a</span>
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #dee2e6' }} />
          <button className="btn btn-primary" onClick={buscarPorRango} disabled={loading}>
            Buscar
          </button>
        </div>
      </div>

      <button className="btn btn-secondary" onClick={listarTodos} disabled={loading} style={{ marginBottom: '1.5rem' }}>
        Listar todos
      </button>

      {/* Results */}
      {loading && <p>Cargando...</p>}

      {resultados.length > 0 && (
        <div>
          <p style={{ color: '#6c757d', marginBottom: '1rem' }}>{resultados.length} resultados</p>
          {resultados.map((sorteo) => (
            <div key={sorteo.id} className="card" style={{ marginBottom: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <div>
                  <span style={{ fontWeight: 700, fontSize: '1.125rem' }}>Sorteo N° {sorteo.numeroSorteo}</span>
                  <span style={{ marginLeft: '1rem', color: '#6c757d' }}>{sorteo.fecha}</span>
                </div>
                {sorteo.pozoMonto && (
                  <span style={{ color: '#27ae60', fontWeight: 600 }}>
                    ${sorteo.pozoMonto.toLocaleString('es-AR')}
                  </span>
                )}
              </div>
              {sorteo.resultados.map((r, idx) => (
                <div key={idx} style={{ padding: '0.5rem 0', borderTop: '1px solid #f1f3f5' }}>
                  <span style={{ fontWeight: 600, marginRight: '1rem' }}>{r.modalidad}</span>
                  {r.numeros.map((num, i) => (
                    <span key={i} style={{
                      display: 'inline-block',
                      width: 32,
                      height: 32,
                      borderRadius: '50%',
                      background: '#4361ee',
                      color: 'white',
                      textAlign: 'center',
                      lineHeight: '32px',
                      marginRight: '4px',
                      fontSize: '0.875rem',
                      fontWeight: 600,
                    }}>
                      {String(num).padStart(2, '0')}
                    </span>
                  ))}
                </div>
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
