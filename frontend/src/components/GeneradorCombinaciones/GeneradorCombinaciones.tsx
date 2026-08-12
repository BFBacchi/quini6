import { useState } from 'react';
import { generadorApi, Combinacion } from '../../services/api';

const MODALIDADES = [
  { value: 'TRADICIONAL', label: 'Tradicional' },
  { value: 'SEGUNDA', label: 'La Segunda' },
  { value: 'REVANCHA', label: 'Revancha' },
  { value: 'SIEMPRE_SALE', label: 'Siempre Sale' },
  { value: 'POZO_EXTRA', label: 'Pozo Extra' },
];

export function GeneradorCombinaciones() {
  const [combinacion, setCombinacion] = useState<Combinacion | null>(null);
  const [loading, setLoading] = useState(false);
  const [modalidad, setModalidad] = useState('TRADICIONAL');

  const generarAleatorio = async () => {
    setLoading(true);
    try {
      const res = await generadorApi.aleatorio();
      setCombinacion(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const generarPonderado = async () => {
    setLoading(true);
    try {
      const res = await generadorApi.ponderado(modalidad);
      setCombinacion(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>
        Generador de Combinaciones
      </h1>
      <p style={{ color: '#6c757d', marginBottom: '2rem' }}>
        Herramienta de <strong>entretenimiento</strong>. Ningún método produce combinaciones
        con mayor probabilidad de ganar.
      </p>

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '2rem' }}>
        <button className="btn btn-primary" onClick={generarAleatorio} disabled={loading}>
          {loading ? 'Generando...' : 'Aleatorio Puro'}
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <select
            value={modalidad}
            onChange={(e) => setModalidad(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #dee2e6' }}
          >
            {MODALIDADES.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
          <button className="btn btn-secondary" onClick={generarPonderado} disabled={loading}>
            Ponderado
          </button>
        </div>
      </div>

      {combinacion && (
        <div className="card" style={{ maxWidth: '500px' }}>
          <div style={{ fontSize: '0.875rem', color: '#6c757d', marginBottom: '0.5rem' }}>
            Método: {combinacion.metodo}
          </div>
          <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem' }}>
            {combinacion.numeros.map((num, idx) => (
              <div
                key={idx}
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: '50%',
                  background: '#4361ee',
                  color: 'white',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.25rem',
                  fontWeight: 700,
                }}
              >
                {String(num).padStart(2, '0')}
              </div>
            ))}
          </div>
          <div style={{
            padding: '1rem',
            background: '#fff3cd',
            borderRadius: '8px',
            color: '#856404',
            fontSize: '0.875rem',
            lineHeight: 1.6,
          }}>
            {combinacion.disclaimer}
          </div>
        </div>
      )}
    </div>
  );
}
