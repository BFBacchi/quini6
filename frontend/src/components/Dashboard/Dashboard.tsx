import { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { estadisticasApi, Frecuencia, ChiCuadrado } from '../../services/api';

const MODALIDADES = [
  { value: 'TRADICIONAL', label: 'Tradicional' },
  { value: 'SEGUNDA', label: 'La Segunda' },
  { value: 'REVANCHA', label: 'Revancha' },
  { value: 'SIEMPRE_SALE', label: 'Siempre Sale' },
  { value: 'POZO_EXTRA', label: 'Pozo Extra' },
];

export function Dashboard() {
  const [modalidad, setModalidad] = useState('TRADICIONAL');
  const [frecuencia, setFrecuencia] = useState<Frecuencia | null>(null);
  const [chiCuadrado, setChiCuadrado] = useState<ChiCuadrado | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [freqRes, chiRes] = await Promise.all([
          estadisticasApi.frecuencia(modalidad),
          estadisticasApi.chiCuadrado(modalidad),
        ]);
        setFrecuencia(freqRes.data);
        setChiCuadrado(chiRes.data);
      } catch (err) {
        console.error('Error fetching stats:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [modalidad]);

  const chartData = frecuencia
    ? Object.values(frecuencia.frecuencias).map((f) => ({
        numero: f.numero,
        absoluta: f.absoluta,
        atraso: f.atraso,
      }))
    : [];

  const top5Calientes = frecuencia
    ? Object.values(frecuencia.frecuencias)
        .sort((a, b) => b.absoluta - a.absoluta)
        .slice(0, 5)
    : [];

  const top5Frios = frecuencia
    ? Object.values(frecuencia.frecuencias)
        .sort((a, b) => b.atraso - a.atraso)
        .slice(0, 5)
    : [];

  return (
    <div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem' }}>
        Dashboard Estadístico
      </h1>

      <div style={{ marginBottom: '1.5rem' }}>
        <label style={{ fontWeight: 600, marginRight: '0.75rem' }}>Modalidad:</label>
        <select
          value={modalidad}
          onChange={(e) => setModalidad(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #dee2e6' }}
        >
          {MODALIDADES.map((m) => (
            <option key={m.value} value={m.value}>{m.label}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <p>Cargando datos...</p>
      ) : (
        <>
          {/* Summary cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
            <div className="card">
              <div style={{ fontSize: '0.875rem', color: '#6c757d' }}>Sorteos analizados</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{frecuencia?.totalSorteos ?? 0}</div>
            </div>
            <div className="card">
              <div style={{ fontSize: '0.875rem', color: '#6c757d' }}>Chi-cuadrado</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{chiCuadrado?.chiCuadrado.toFixed(2) ?? '-'}</div>
            </div>
            <div className="card">
              <div style={{ fontSize: '0.875rem', color: '#6c757d' }}>p-valor</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{chiCuadrado?.pValue.toFixed(4) ?? '-'}</div>
            </div>
          </div>

          {/* Chart */}
          <div className="card" style={{ marginBottom: '2rem' }}>
            <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem' }}>
              Frecuencia por número
            </h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="numero" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="absoluta" fill="#4361ee" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Hot / Cold */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
            <div className="card">
              <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', color: '#e74c3c' }}>
                Más frecuentes (Calientes)
              </h2>
              {top5Calientes.map((f) => (
                <div key={f.numero} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f3f5' }}>
                  <span style={{ fontWeight: 600 }}>N° {String(f.numero).padStart(2, '0')}</span>
                  <span>{f.absoluta} veces ({(f.relativa * 100).toFixed(1)}%)</span>
                </div>
              ))}
            </div>
            <div className="card">
              <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', color: '#3498db' }}>
                Mayor atraso (Fríos)
              </h2>
              {top5Frios.map((f) => (
                <div key={f.numero} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f3f5' }}>
                  <span style={{ fontWeight: 600 }}>N° {String(f.numero).padStart(2, '0')}</span>
                  <span>{f.atraso} sorteos sin salir</span>
                </div>
              ))}
            </div>
          </div>

          {/* Chi-cuadrado interpretation */}
          {chiCuadrado && (
            <div className="card" style={{ borderLeft: '4px solid #4361ee' }}>
              <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.5rem' }}>
                Test de uniformidad (Chi-cuadrado)
              </h2>
              <p style={{ color: '#495057', lineHeight: 1.6 }}>{chiCuadrado.interpretacion}</p>
            </div>
          )}

          {/* Disclaimer */}
          <div style={{ marginTop: '2rem', padding: '1rem', background: '#fff3cd', borderRadius: '8px', color: '#856404', fontSize: '0.875rem' }}>
            <strong>Disclaimer:</strong> {frecuencia?.disclaimer}
          </div>
        </>
      )}
    </div>
  );
}
