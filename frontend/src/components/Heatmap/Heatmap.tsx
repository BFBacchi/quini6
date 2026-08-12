import { useState, useEffect } from 'react';
import { estadisticasApi, Frecuencia } from '../../services/api';

const MODALIDADES = [
  { value: 'TRADICIONAL', label: 'Tradicional' },
  { value: 'SEGUNDA', label: 'La Segunda' },
  { value: 'REVANCHA', label: 'Revancha' },
  { value: 'SIEMPRE_SALE', label: 'Siempre Sale' },
  { value: 'POZO_EXTRA', label: 'Pozo Extra' },
];

function getColor(intensity: number): string {
  if (intensity > 0.035) return '#e74c3c';
  if (intensity > 0.028) return '#e67e22';
  if (intensity > 0.022) return '#f1c40f';
  if (intensity > 0.018) return '#2ecc71';
  return '#3498db';
}

export function Heatmap() {
  const [modalidad, setModalidad] = useState('TRADICIONAL');
  const [frecuencia, setFrecuencia] = useState<Frecuencia | null>(null);
  const [loading, setLoading] = useState(true);
  const [hovered, setHovered] = useState<number | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await estadisticasApi.frecuencia(modalidad);
        setFrecuencia(res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [modalidad]);

  const numeros = frecuencia
    ? Object.values(frecuencia.frecuencias).sort((a, b) => a.numero - b.numero)
    : [];

  return (
    <div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem' }}>
        Heatmap de Frecuencias
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
        <p>Cargando heatmap...</p>
      ) : (
        <>
          {/* Legend */}
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', fontSize: '0.875rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <div style={{ width: 16, height: 16, borderRadius: 3, background: '#e74c3c' }} /> Muy frecuente
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <div style={{ width: 16, height: 16, borderRadius: 3, background: '#f1c40f' }} /> Promedio
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <div style={{ width: 16, height: 16, borderRadius: 3, background: '#3498db' }} /> Poco frecuente
            </div>
          </div>

          {/* Heatmap grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, 56px)', gap: '4px', marginBottom: '2rem' }}>
            {numeros.map((f) => (
              <div
                key={f.numero}
                className="heatmap-cell"
                style={{
                  background: getColor(f.relativa),
                  color: 'white',
                  position: 'relative',
                }}
                onMouseEnter={() => setHovered(f.numero)}
                onMouseLeave={() => setHovered(null)}
              >
                {String(f.numero).padStart(2, '0')}
                {hovered === f.numero && (
                  <div style={{
                    position: 'absolute',
                    bottom: '110%',
                    left: '50%',
                    transform: 'translateX(-50%)',
                    background: '#1a1a2e',
                    color: 'white',
                    padding: '0.5rem',
                    borderRadius: '6px',
                    fontSize: '0.75rem',
                    whiteSpace: 'nowrap',
                    zIndex: 10,
                  }}>
                    <div>N° {f.numero}</div>
                    <div>Veces: {f.absoluta}</div>
                    <div>Frecuencia: {(f.relativa * 100).toFixed(1)}%</div>
                    <div>Atraso: {f.atraso} sorteos</div>
                  </div>
                )}
              </div>
            ))}
          </div>

          <div style={{ padding: '1rem', background: '#fff3cd', borderRadius: '8px', color: '#856404', fontSize: '0.875rem' }}>
            <strong>Disclaimer:</strong> Colores basados en frecuencia histórica. Los sorteos son eventos independientes.
            Un número "frío" no tiene más ni menos probabilidad de salir que uno "caliente".
          </div>
        </>
      )}
    </div>
  );
}
