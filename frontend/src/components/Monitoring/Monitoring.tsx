import { useState, useEffect, useCallback } from 'react';
import {
  MonitoringHealth,
  MonitoringDatabase,
  MonitoringElasticsearch,
  MonitoringRabbitMQ,
  Sorteo,
  TableDataResponse,
  SchemaResponse,
  monitoringApi,
  sorteosApi,
} from '../../services/api';

type Tab = 'overview' | 'postgresql' | 'elasticsearch' | 'rabbitmq' | 'sorteos';
type DbSubView = 'tables' | 'er' | 'schema';

const TABLE_COLORS: Record<string, string> = {
  sorteos: '#4361ee',
  resultados: '#7c3aed',
  usuarios: '#06b6d4',
  audit_log: '#f59e0b',
  numeros_sorteados: '#27ae60',
};

function modalidadColor(m: string): string {
  switch (m) {
    case 'TRADICIONAL': return '#4361ee';
    case 'SEGUNDA': return '#7c3aed';
    case 'REVANCHA': return '#e74c3c';
    case 'SIEMPRE_SALE': return '#f59e0b';
    case 'POZO_EXTRA': return '#27ae60';
    default: return '#6c757d';
  }
}

const styles = {
  container: { padding: '1.5rem', fontFamily: 'system-ui, -apple-system, sans-serif' } as React.CSSProperties,
  title: { fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem' } as React.CSSProperties,
  tabRow: { display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '2px solid #e9ecef', paddingBottom: '0.5rem', flexWrap: 'wrap' as const } as React.CSSProperties,
  tab: (active: boolean): React.CSSProperties => ({
    padding: '0.5rem 1.25rem',
    borderRadius: '8px 8px 0 0',
    border: 'none',
    cursor: 'pointer',
    fontWeight: active ? 700 : 400,
    background: active ? '#4361ee' : 'transparent',
    color: active ? '#fff' : '#6c757d',
    fontSize: '0.9rem',
    transition: 'all 0.15s',
  }),
  card: { background: '#fff', borderRadius: '10px', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', padding: '1.25rem', marginBottom: '1rem' } as React.CSSProperties,
  grid: (cols: string): React.CSSProperties => ({
    display: 'grid',
    gridTemplateColumns: cols,
    gap: '1rem',
    marginBottom: '1.5rem',
  }),
  subTabRow: { display: 'flex', gap: '0.5rem', marginBottom: '1.25rem' } as React.CSSProperties,
  subTab: (active: boolean): React.CSSProperties => ({
    padding: '0.4rem 1rem',
    borderRadius: '6px',
    border: '1px solid #dee2e6',
    cursor: 'pointer',
    fontWeight: active ? 600 : 400,
    background: active ? '#4361ee' : '#f8f9fa',
    color: active ? '#fff' : '#495057',
    fontSize: '0.85rem',
  }),
  badge: (color: string): React.CSSProperties => ({
    display: 'inline-block',
    padding: '0.2rem 0.6rem',
    borderRadius: '12px',
    background: color + '18',
    color: color,
    fontWeight: 600,
    fontSize: '0.8rem',
  }),
  table: { width: '100%', borderCollapse: 'collapse' as const, fontSize: '0.85rem' } as React.CSSProperties,
  th: { textAlign: 'left' as const, padding: '0.6rem 0.75rem', borderBottom: '2px solid #dee2e6', fontWeight: 600, color: '#495057', cursor: 'pointer', userSelect: 'none' as const } as React.CSSProperties,
  td: { padding: '0.5rem 0.75rem', borderBottom: '1px solid #f1f3f5' } as React.CSSProperties,
  pagination: { display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '1rem', fontSize: '0.875rem', color: '#6c757d' } as React.CSSProperties,
  btn: (disabled: boolean): React.CSSProperties => ({
    padding: '0.4rem 1rem',
    borderRadius: '6px',
    border: '1px solid #dee2e6',
    background: disabled ? '#f8f9fa' : '#fff',
    cursor: disabled ? 'not-allowed' : 'pointer',
    fontWeight: 500,
    color: disabled ? '#adb5bd' : '#495057',
  }),
  sorteoRow: { display: 'flex', justifyContent: 'space-between', padding: '0.5rem 0', borderBottom: '1px solid #f1f3f5' } as React.CSSProperties,
};

// ─── OverviewTab ───────────────────────────────────────

function ServiceCard({ label, status }: { label: string; status: string }) {
  const isUp = status.toLowerCase() === 'up' || status.toLowerCase() === 'connected' || status.toLowerCase() === 'running';
  return (
    <div style={styles.card}>
      <div style={{ fontSize: '0.875rem', color: '#6c757d', marginBottom: '0.5rem' }}>{label}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: isUp ? '#27ae60' : '#e74c3c', display: 'inline-block' }} />
        <span style={{ fontWeight: 600, fontSize: '1rem' }}>{status}</span>
      </div>
    </div>
  );
}

function OverviewTab({ health, db }: { health: MonitoringHealth | null; db: MonitoringDatabase | null }) {
  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Servicios</h2>
      <div style={styles.grid('repeat(auto-fit, minmax(200px, 1fr))')}>
        <ServiceCard label="Estado General" status={health?.status ?? '—'} />
        <ServiceCard label="PostgreSQL" status={health?.postgres ?? '—'} />
        <ServiceCard label="Elasticsearch" status={health?.elasticsearch ?? '—'} />
        <ServiceCard label="RabbitMQ" status={health?.rabbitmq ?? '—'} />
      </div>
      {db && (
        <>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Base de Datos</h2>
          <div style={styles.grid('repeat(auto-fit, minmax(160px, 1fr))')}>
            <StatCard label="Sorteos" value={db.sorteos} color="#4361ee" />
            <StatCard label="Resultados" value={db.resultados} color="#7c3aed" />
            <StatCard label="Usuarios" value={db.usuarios} color="#06b6d4" />
            <StatCard label="Audit Logs" value={db.auditLogs} color="#f59e0b" />
            <StatCard label="Último Sorteo" value={db.ultimoSorteo} color="#27ae60" />
          </div>
        </>
      )}
    </div>
  );
}

// ─── Stat helpers ──────────────────────────────────────

function Stat({ label, value, color }: { label: string; value: number | string; color: string }) {
  return (
    <div style={{ textAlign: 'center', padding: '0.75rem' }}>
      <div style={{ fontSize: '1.5rem', fontWeight: 700, color }}>{value}</div>
      <div style={{ fontSize: '0.8rem', color: '#6c757d', marginTop: '0.25rem' }}>{label}</div>
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={styles.card}>
      <Stat label={label} value={value.toLocaleString()} color={color} />
    </div>
  );
}

// ─── DatabaseTab ───────────────────────────────────────

function DatabaseTab({
  dbInfo,
  subView,
  setSubView,
}: {
  dbInfo: MonitoringDatabase | null;
  subView: DbSubView;
  setSubView: (v: DbSubView) => void;
}) {
  const tableCards: { name: string; rows: number; color: string; label: string }[] = [
    { name: 'sorteos', rows: dbInfo?.sorteos ?? 0, color: TABLE_COLORS.sorteos, label: 'Sorteos' },
    { name: 'resultados', rows: dbInfo?.resultados ?? 0, color: TABLE_COLORS.resultados, label: 'Resultados' },
    { name: 'usuarios', rows: dbInfo?.usuarios ?? 0, color: TABLE_COLORS.usuarios, label: 'Usuarios' },
    { name: 'audit_log', rows: dbInfo?.auditLogs ?? 0, color: TABLE_COLORS.audit_log, label: 'Audit Log' },
    { name: 'numeros_sorteados', rows: 0, color: TABLE_COLORS.numeros_sorteados, label: 'Números Sorteados' },
  ];

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Estadísticas de Tablas</h2>
      <div style={styles.grid('repeat(auto-fit, minmax(160px, 1fr))')}>
        {tableCards.map((t) => (
          <div key={t.name} style={styles.card}>
            <div style={{ fontSize: '0.8rem', color: '#6c757d', marginBottom: '0.25rem' }}>{t.label}</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: t.color }}>{t.rows.toLocaleString()}</div>
            <div style={{ fontSize: '0.75rem', color: '#adb5bd', marginTop: '0.15rem' }}>{t.name}</div>
          </div>
        ))}
      </div>

      <div style={styles.subTabRow}>
        <button style={styles.subTab(subView === 'tables')} onClick={() => setSubView('tables')}>Tablas</button>
        <button style={styles.subTab(subView === 'er')} onClick={() => setSubView('er')}>Diagrama ER</button>
        <button style={styles.subTab(subView === 'schema')} onClick={() => setSubView('schema')}>Schema SQL</button>
      </div>

      {subView === 'tables' && <TableView />}
      {subView === 'er' && <ERDiagram />}
      {subView === 'schema' && <SchemaView />}
    </div>
  );
}

// ─── TableView ─────────────────────────────────────────

function TableView() {
  const [tables] = useState<string[]>(['sorteos', 'resultados', 'usuarios', 'audit_log', 'numeros_sorteados']);
  const [selectedTable, setSelectedTable] = useState<string>('sorteos');
  const [tableData, setTableData] = useState<TableDataResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [sortCol, setSortCol] = useState<number | null>(null);
  const [sortAsc, setSortAsc] = useState(true);
  const size = 50;

  const fetchData = useCallback(async (table: string, p: number) => {
    setLoading(true);
    try {
      const res = await monitoringApi.tableData(table, p, size);
      setTableData(res.data);
    } catch (err) {
      console.error('Error fetching table data:', err);
      setTableData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(selectedTable, page);
  }, [selectedTable, page, fetchData]);

  const sortedData = tableData
    ? [...tableData.data].sort((a, b) => {
        if (sortCol === null) return 0;
        const av = a[sortCol];
        const bv = b[sortCol];
        if (av == null && bv == null) return 0;
        if (av == null) return 1;
        if (bv == null) return -1;
        if (typeof av === 'number' && typeof bv === 'number') return sortAsc ? av - bv : bv - av;
        const cmp = String(av).localeCompare(String(bv));
        return sortAsc ? cmp : -cmp;
      })
    : [];

  const totalPages = tableData ? Math.ceil(tableData.total / size) : 0;

  const handleSort = (idx: number) => {
    if (sortCol === idx) {
      setSortAsc(!sortAsc);
    } else {
      setSortCol(idx);
      setSortAsc(true);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
        {tables.map((t) => (
          <button
            key={t}
            onClick={() => { setSelectedTable(t); setPage(0); setSortCol(null); }}
            style={{
              padding: '0.4rem 0.9rem',
              borderRadius: '6px',
              border: selectedTable === t ? `2px solid ${TABLE_COLORS[t] || '#4361ee'}` : '1px solid #dee2e6',
              background: selectedTable === t ? (TABLE_COLORS[t] || '#4361ee') + '12' : '#fff',
              cursor: 'pointer',
              fontWeight: selectedTable === t ? 600 : 400,
              color: selectedTable === t ? (TABLE_COLORS[t] || '#4361ee') : '#495057',
              fontSize: '0.85rem',
            }}
          >
            {t}
          </button>
        ))}
      </div>

      {loading ? (
        <p style={{ color: '#6c757d' }}>Cargando datos...</p>
      ) : tableData ? (
        <>
          <div style={{ overflowX: 'auto' }}>
            <table style={styles.table}>
              <thead>
                <tr>
                  {tableData.columns.map((col, i) => (
                    <th key={col} style={styles.th} onClick={() => handleSort(i)}>
                      {col} {sortCol === i ? (sortAsc ? '▲' : '▼') : ''}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {sortedData.map((row, ri) => (
                  <tr key={ri} style={{ background: ri % 2 === 0 ? '#fff' : '#f8f9fa' }}>
                    {row.map((cell, ci) => (
                      <td key={ci} style={styles.td}>
                        {cell === null ? <span style={{ color: '#adb5bd', fontStyle: 'italic' }}>NULL</span> : String(cell)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={styles.pagination}>
            <button disabled={page === 0} style={styles.btn(page === 0)} onClick={() => setPage(page - 1)}>
              ← Anterior
            </button>
            <span>Página {page + 1} de {totalPages} ({tableData.total.toLocaleString()} registros)</span>
            <button disabled={page >= totalPages - 1} style={styles.btn(page >= totalPages - 1)} onClick={() => setPage(page + 1)}>
              Siguiente →
            </button>
          </div>
        </>
      ) : (
        <p style={{ color: '#6c757d' }}>Selecciona una tabla.</p>
      )}
    </div>
  );
}

// ─── ER Diagram ────────────────────────────────────────

interface ERTableDef {
  name: string;
  label: string;
  color: string;
  x: number;
  y: number;
  columns: { name: string; pk?: boolean; fk?: boolean; type: string }[];
}

const ER_TABLES: ERTableDef[] = [
  {
    name: 'usuarios', label: 'usuarios', color: TABLE_COLORS.usuarios, x: 30, y: 60,
    columns: [
      { name: 'id', pk: true, type: 'UUID' },
      { name: 'username', type: 'VARCHAR' },
      { name: 'password_hash', type: 'VARCHAR' },
      { name: 'role', type: 'VARCHAR' },
      { name: 'created_at', type: 'TIMESTAMP' },
    ],
  },
  {
    name: 'sorteos', label: 'sorteos', color: TABLE_COLORS.sorteos, x: 300, y: 60,
    columns: [
      { name: 'id', pk: true, type: 'UUID' },
      { name: 'numero_sorteo', type: 'INTEGER' },
      { name: 'fecha', type: 'DATE' },
      { name: 'pozo_monto', type: 'DECIMAL' },
      { name: 'created_at', type: 'TIMESTAMP' },
    ],
  },
  {
    name: 'resultados', label: 'resultados', color: TABLE_COLORS.resultados, x: 600, y: 60,
    columns: [
      { name: 'id', pk: true, type: 'UUID' },
      { name: 'sorteo_id', fk: true, type: 'UUID' },
      { name: 'modalidad', type: 'VARCHAR' },
      { name: 'numeros', type: 'INTEGER[]' },
      { name: 'ganadores6', type: 'INTEGER' },
      { name: 'monto6', type: 'DECIMAL' },
      { name: 'ganadores5', type: 'INTEGER' },
      { name: 'monto5', type: 'DECIMAL' },
      { name: 'ganadores4', type: 'INTEGER' },
      { name: 'monto4', type: 'DECIMAL' },
    ],
  },
  {
    name: 'audit_log', label: 'audit_log', color: TABLE_COLORS.audit_log, x: 30, y: 340,
    columns: [
      { name: 'id', pk: true, type: 'BIGSERIAL' },
      { name: 'usuario_id', fk: true, type: 'UUID' },
      { name: 'action', type: 'VARCHAR' },
      { name: 'details', type: 'TEXT' },
      { name: 'created_at', type: 'TIMESTAMP' },
    ],
  },
  {
    name: 'numeros_sorteados', label: 'numeros_sorteados', color: TABLE_COLORS.numeros_sorteados, x: 600, y: 340,
    columns: [
      { name: 'id', pk: true, type: 'BIGSERIAL' },
      { name: 'sorteo_id', fk: true, type: 'UUID' },
      { name: 'resultado_id', fk: true, type: 'UUID' },
      { name: 'numero', type: 'INTEGER' },
      { name: 'posicion', type: 'INTEGER' },
    ],
  },
];

interface FKDef { from: string; fromCol: string; to: string; toCol: string; }

const FK_RELS: FKDef[] = [
  { from: 'resultados', fromCol: 'sorteo_id', to: 'sorteos', toCol: 'id' },
  { from: 'audit_log', fromCol: 'usuario_id', to: 'usuarios', toCol: 'id' },
  { from: 'numeros_sorteados', fromCol: 'sorteo_id', to: 'sorteos', toCol: 'id' },
  { from: 'numeros_sorteados', fromCol: 'resultado_id', to: 'resultados', toCol: 'id' },
];

const TABLE_W = 190;
const ROW_H = 18;
const HEADER_H = 26;
const PAD = 10;

function tableHeight(t: ERTableDef) {
  return HEADER_H + t.columns.length * ROW_H + PAD;
}

function getAnchor(table: ERTableDef, side: 'right' | 'left' | 'top' | 'bottom'): { x: number; y: number } {
  const h = tableHeight(table);
  switch (side) {
    case 'right': return { x: table.x + TABLE_W, y: table.y + h / 2 };
    case 'left': return { x: table.x, y: table.y + h / 2 };
    case 'top': return { x: table.x + TABLE_W / 2, y: table.y };
    case 'bottom': return { x: table.x + TABLE_W / 2, y: table.y + h };
  }
}

function fkPath(rel: FKDef): { from: { x: number; y: number }; to: { x: number; y: number }; path: string } {
  const tFrom = ER_TABLES.find((t) => t.name === rel.from)!;
  const tTo = ER_TABLES.find((t) => t.name === rel.to)!;

  let fromAnchor: { x: number; y: number };
  let toAnchor: { x: number; y: number };

  if (tFrom.x < tTo.x) {
    fromAnchor = getAnchor(tFrom, 'right');
    toAnchor = getAnchor(tTo, 'left');
  } else if (tFrom.x > tTo.x) {
    fromAnchor = getAnchor(tFrom, 'left');
    toAnchor = getAnchor(tTo, 'right');
  } else {
    fromAnchor = getAnchor(tFrom, 'bottom');
    toAnchor = getAnchor(tTo, 'top');
  }

  const dx = Math.abs(toAnchor.x - fromAnchor.x) * 0.5;
  const path = `M ${fromAnchor.x} ${fromAnchor.y} C ${fromAnchor.x + (toAnchor.x > fromAnchor.x ? dx : -dx)} ${fromAnchor.y}, ${toAnchor.x + (toAnchor.x > fromAnchor.x ? -dx : dx)} ${toAnchor.y}, ${toAnchor.x} ${toAnchor.y}`;

  return { from: fromAnchor, to: toAnchor, path };
}

function ERDiagram() {
  const [hoveredTable, setHoveredTable] = useState<string | null>(null);

  const svgWidth = 900;
  const svgHeight = 560;

  return (
    <div style={{ ...styles.card, overflowX: 'auto' }}>
      <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Diagrama Entidad-Relación</h3>
      <svg width={svgWidth} height={svgHeight} style={{ border: '1px solid #e9ecef', borderRadius: '8px', background: '#fafbfc' }}>
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#6c757d" />
          </marker>
        </defs>

        {FK_RELS.map((rel) => {
          const curve = fkPath(rel);
          const isHighlighted = hoveredTable === rel.from || hoveredTable === rel.to;
          return (
            <g key={`${rel.from}-${rel.fromCol}-${rel.to}-${rel.toCol}`}>
              <path d={curve.path} fill="none" stroke={isHighlighted ? '#4361ee' : '#adb5bd'} strokeWidth={isHighlighted ? 2.5 : 1.5} strokeDasharray={isHighlighted ? 'none' : '6,3'} markerEnd="url(#arrow)" />
              <text x={(curve.from.x + curve.to.x) / 2} y={(curve.from.y + curve.to.y) / 2 - 6} textAnchor="middle" fontSize="9" fill="#6c757d">
                {rel.fromCol}
              </text>
            </g>
          );
        })}

        {ER_TABLES.map((t) => {
          const h = tableHeight(t);
          const isHovered = hoveredTable === t.name;
          return (
            <g key={t.name} onMouseEnter={() => setHoveredTable(t.name)} onMouseLeave={() => setHoveredTable(null)} style={{ cursor: 'pointer' }}>
              <rect x={t.x} y={t.y} width={TABLE_W} height={h} rx="8" ry="8" fill={isHovered ? t.color + '18' : '#fff'} stroke={t.color} strokeWidth={isHovered ? 2.5 : 1.5} />
              <rect x={t.x} y={t.y} width={TABLE_W} height={HEADER_H} rx="8" ry="8" fill={t.color} />
              <rect x={t.x} y={t.y + HEADER_H - 8} width={TABLE_W} height={8} fill={t.color} />
              <text x={t.x + TABLE_W / 2} y={t.y + HEADER_H / 2 + 4} textAnchor="middle" fontSize="12" fontWeight="700" fill="#fff">
                {t.label}
              </text>
              {t.columns.map((col, ci) => {
                const cy = t.y + HEADER_H + ci * ROW_H + ROW_H / 2 + 2;
                const marker = col.pk ? '🔑' : col.fk ? '🔗' : '';
                return (
                  <g key={col.name}>
                    <text x={t.x + 12} y={cy} fontSize="10" fill="#495057" fontFamily="monospace">
                      {col.name}
                    </text>
                    <text x={t.x + TABLE_W - 10} y={cy} textAnchor="end" fontSize="9" fill="#adb5bd">
                      {col.type}
                    </text>
                    {marker && (
                      <text x={t.x + TABLE_W - 10 - (col.type.length * 5.5 + 4)} y={cy} fontSize="9" fill={col.pk ? '#f59e0b' : '#06b6d4'}>
                        {marker}
                      </text>
                    )}
                  </g>
                );
              })}
              {t.columns.length > 0 && (
                <line x1={t.x} y1={t.y + HEADER_H} x2={t.x + TABLE_W} y2={t.y + HEADER_H} stroke="#dee2e6" strokeWidth="1" />
              )}
            </g>
          );
        })}
      </svg>
    </div>
  );
}

// ─── SchemaView ────────────────────────────────────────

function SchemaView() {
  const [schema, setSchema] = useState<SchemaResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedTable, setExpandedTable] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await monitoringApi.schema();
        setSchema(res.data);
        if (res.data.tables.length > 0) setExpandedTable(res.data.tables[0].table);
      } catch (err) {
        console.error('Error loading schema:', err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <p style={{ color: '#6c757d' }}>Cargando schema...</p>;
  if (!schema) return <p style={{ color: '#6c757d' }}>No se pudo cargar el schema.</p>;

  return (
    <div>
      {schema.tables.map((t) => {
        const color = TABLE_COLORS[t.table] || '#6c757d';
        const isExpanded = expandedTable === t.table;
        const pkCols = new Set(
          schema.constraints.filter((c) => c.type === 'PRIMARY KEY' && c.table === t.table).map((c) => c.column)
        );
        const fkCols = new Map(
          schema.constraints.filter((c) => c.type === 'FOREIGN KEY' && c.table === t.table).map((c) => [c.column, `${c.foreignTable}.${c.foreignColumn}`])
        );
        const indexes = schema.indexes.filter((ix) => ix.table === t.table);

        return (
          <div key={t.table} style={{ ...styles.card, borderLeft: `4px solid ${color}` }}>
            <div
              onClick={() => setExpandedTable(isExpanded ? null : t.table)}
              style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}
            >
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color }}>{t.table}</h3>
              <span style={{ fontSize: '0.85rem', color: '#6c757d' }}>{isExpanded ? '▲' : '▼'} {t.columns.length} columnas</span>
            </div>
            {isExpanded && (
              <div style={{ marginTop: '0.75rem' }}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th style={styles.th}>Columna</th>
                      <th style={styles.th}>Tipo</th>
                      <th style={styles.th}>Nullable</th>
                      <th style={styles.th}>Default</th>
                      <th style={styles.th}>Key</th>
                    </tr>
                  </thead>
                  <tbody>
                    {t.columns.map((col) => {
                      const isPk = pkCols.has(col.name);
                      const fkRef = fkCols.get(col.name);
                      return (
                        <tr key={col.name}>
                          <td style={styles.td}><code style={{ fontWeight: 600 }}>{col.name}</code></td>
                          <td style={styles.td}><span style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: '#6c757d' }}>{col.type}{col.maxLength ? `(${col.maxLength})` : ''}</span></td>
                          <td style={styles.td}>{col.nullable ? '✓' : '—'}</td>
                          <td style={styles.td}><span style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: '#6c757d' }}>{col.default ?? '—'}</span></td>
                          <td style={styles.td}>
                            {isPk && <span style={styles.badge('#f59e0b')}>PK</span>}
                            {fkRef && <span style={{ ...styles.badge('#06b6d4'), marginLeft: isPk ? '0.25rem' : 0 }}>FK → {fkRef}</span>}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                {indexes.length > 0 && (
                  <div style={{ marginTop: '0.75rem' }}>
                    <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: '#495057', marginBottom: '0.4rem' }}>Índices</h4>
                    {indexes.map((ix) => (
                      <div key={ix.name} style={{ ...styles.sorteoRow, fontSize: '0.8rem' }}>
                        <code style={{ color: '#4361ee', fontWeight: 500 }}>{ix.name}</code>
                        <code style={{ color: '#6c757d', marginLeft: '1rem' }}>{ix.definition}</code>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ─── ElasticsearchTab ──────────────────────────────────

function ElasticsearchTab({ data }: { data: MonitoringElasticsearch | null }) {
  if (!data) return <p style={{ color: '#6c757d' }}>Cargando datos de Elasticsearch...</p>;

  const indices = Array.isArray(data.indices) ? data.indices : [];
  const cluster = data.cluster ?? {};

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Elasticsearch</h2>
      <div style={styles.card}>
        <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Cluster Info</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.5rem' }}>
          {Object.entries(cluster).map(([k, v]) => (
            <div key={k} style={{ ...styles.sorteoRow, padding: '0.3rem 0' }}>
              <span style={{ fontWeight: 500, color: '#495057' }}>{k}</span>
              <span style={{ fontFamily: 'monospace', color: '#6c757d' }}>{String(v)}</span>
            </div>
          ))}
        </div>
      </div>

      {indices.length > 0 && (
        <div style={styles.card}>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Índices ({indices.length})</h3>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Índice</th>
                <th style={styles.th}>Documentos</th>
                <th style={styles.th}>Tamaño</th>
              </tr>
            </thead>
            <tbody>
              {indices.map((idx: any, i: number) => (
                <tr key={idx.name || i}>
                  <td style={styles.td}><code style={{ color: '#4361ee', fontWeight: 500 }}>{idx.name ?? String(idx)}</code></td>
                  <td style={styles.td}>{idx.docs_count ?? idx.docs?.count ?? '—'}</td>
                  <td style={styles.td}>{idx.store_size ?? idx.store?.size_in_bytes ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data.error && (
        <div style={{ ...styles.card, borderLeft: '4px solid #e74c3c', color: '#e74c3c' }}>
          {data.error}
        </div>
      )}
    </div>
  );
}

// ─── RabbitMQTab ───────────────────────────────────────

function RabbitMQTab({ data }: { data: MonitoringRabbitMQ | null }) {
  if (!data) return <p style={{ color: '#6c757d' }}>Cargando datos de RabbitMQ...</p>;

  const queues = Array.isArray(data.queues) ? data.queues : [];
  const overview = data.overview ?? {};

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>RabbitMQ</h2>
      <div style={styles.card}>
        <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Overview</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.5rem' }}>
          {Object.entries(overview).map(([k, v]) => (
            <div key={k} style={{ ...styles.sorteoRow, padding: '0.3rem 0' }}>
              <span style={{ fontWeight: 500, color: '#495057' }}>{k}</span>
              <span style={{ fontFamily: 'monospace', color: '#6c757d' }}>{String(v)}</span>
            </div>
          ))}
        </div>
      </div>

      {queues.length > 0 && (
        <div style={styles.card}>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Colas ({queues.length})</h3>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Cola</th>
                <th style={styles.th}>Mensajes</th>
                <th style={styles.th}>Consumers</th>
                <th style={styles.th}>Estado</th>
              </tr>
            </thead>
            <tbody>
              {queues.map((q: any, i: number) => (
                <tr key={q.name || i}>
                  <td style={styles.td}><code style={{ color: '#4361ee', fontWeight: 500 }}>{q.name ?? String(q)}</code></td>
                  <td style={styles.td}>{q.messages ?? q.messages_ready ?? '—'}</td>
                  <td style={styles.td}>{q.consumers ?? 0}</td>
                  <td style={styles.td}>
                    <span style={styles.badge(q.state === 'running' ? '#27ae60' : '#f59e0b')}>{q.state ?? '—'}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data.error && (
        <div style={{ ...styles.card, borderLeft: '4px solid #e74c3c', color: '#e74c3c' }}>
          {data.error}
        </div>
      )}
    </div>
  );
}

// ─── SorteosTab ────────────────────────────────────────

function SorteosTab() {
  const [sorteos, setSorteos] = useState<Sorteo[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortCol, setSortCol] = useState<string>('numeroSorteo');
  const [sortAsc, setSortAsc] = useState(false);

  useEffect(() => {
    sorteosApi.listar()
      .then((res) => setSorteos(res.data))
      .catch((err) => console.error('Error loading sorteos:', err))
      .finally(() => setLoading(false));
  }, []);

  const handleSort = (key: string) => {
    if (sortCol === key) setSortAsc(!sortAsc);
    else { setSortCol(key); setSortAsc(true); }
  };

  const sorted = [...sorteos].sort((a, b) => {
    let av: any, bv: any;
    switch (sortCol) {
      case 'numeroSorteo': av = a.numeroSorteo; bv = b.numeroSorteo; break;
      case 'fecha': av = a.fecha; bv = b.fecha; break;
      case 'pozoMonto': av = a.pozoMonto ?? 0; bv = b.pozoMonto ?? 0; break;
      default: av = a.numeroSorteo; bv = b.numeroSorteo;
    }
    if (typeof av === 'string') return sortAsc ? av.localeCompare(bv) : bv.localeCompare(av);
    return sortAsc ? av - bv : bv - av;
  });

  if (loading) return <p style={{ color: '#6c757d' }}>Cargando sorteos...</p>;

  const cols: { key: string; label: string }[] = [
    { key: 'numeroSorteo', label: 'N° Sorteo' },
    { key: 'fecha', label: 'Fecha' },
    { key: 'pozoMonto', label: 'Pozo' },
  ];

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>Sorteos ({sorteos.length})</h2>
      <div style={{ overflowX: 'auto' }}>
        <table style={styles.table}>
          <thead>
            <tr>
              {cols.map((c) => (
                <th key={c.key} style={styles.th} onClick={() => handleSort(c.key)}>
                  {c.label} {sortCol === c.key ? (sortAsc ? '▲' : '▼') : ''}
                </th>
              ))}
              <th style={styles.th}>Modalidades</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((s) => (
              <tr key={s.id}>
                <td style={styles.td}><strong>#{s.numeroSorteo}</strong></td>
                <td style={styles.td}>{new Date(s.fecha).toLocaleDateString('es-AR')}</td>
                <td style={styles.td}>{s.pozoMonto != null ? `$${s.pozoMonto.toLocaleString()}` : '—'}</td>
                <td style={styles.td}>
                  <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap' }}>
                    {s.resultados.map((r) => (
                      <span key={r.modalidad} style={styles.badge(modalidadColor(r.modalidad))}>
                        {r.modalidad}
                      </span>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Main Component ────────────────────────────────────

export function Monitoring() {
  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const [dbSubView, setDbSubView] = useState<DbSubView>('tables');
  const [health, setHealth] = useState<MonitoringHealth | null>(null);
  const [dbInfo, setDbInfo] = useState<MonitoringDatabase | null>(null);
  const [esData, setEsData] = useState<MonitoringElasticsearch | null>(null);
  const [rmqData, setRmqData] = useState<MonitoringRabbitMQ | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true);
      try {
        const [h, db, es, rmq] = await Promise.allSettled([
          monitoringApi.health(),
          monitoringApi.database(),
          monitoringApi.elasticsearch(),
          monitoringApi.rabbitmq(),
        ]);
        if (h.status === 'fulfilled') setHealth(h.value.data);
        if (db.status === 'fulfilled') setDbInfo(db.value.data);
        if (es.status === 'fulfilled') setEsData(es.value.data);
        if (rmq.status === 'fulfilled') setRmqData(rmq.value.data);
      } catch (err) {
        console.error('Error fetching monitoring data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  const tabs: { key: Tab; label: string }[] = [
    { key: 'overview', label: 'Overview' },
    { key: 'postgresql', label: 'PostgreSQL' },
    { key: 'elasticsearch', label: 'Elasticsearch' },
    { key: 'rabbitmq', label: 'RabbitMQ' },
    { key: 'sorteos', label: 'Sorteos' },
  ];

  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Monitoreo del Sistema</h1>

      <div style={styles.tabRow}>
        {tabs.map((t) => (
          <button key={t.key} style={styles.tab(activeTab === t.key)} onClick={() => setActiveTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {loading ? (
        <p style={{ color: '#6c757d' }}>Cargando datos de monitoreo...</p>
      ) : (
        <>
          {activeTab === 'overview' && <OverviewTab health={health} db={dbInfo} />}
          {activeTab === 'postgresql' && (
            <DatabaseTab dbInfo={dbInfo} subView={dbSubView} setSubView={setDbSubView} />
          )}
          {activeTab === 'elasticsearch' && <ElasticsearchTab data={esData} />}
          {activeTab === 'rabbitmq' && <RabbitMQTab data={rmqData} />}
          {activeTab === 'sorteos' && <SorteosTab />}
        </>
      )}

      <div style={{ marginTop: '2rem', padding: '1rem', background: '#fff3cd', borderRadius: '8px', color: '#856404', fontSize: '0.85rem' }}>
        <strong>Disclaimer:</strong> Este panel muestra datos operativos e históricos del sistema. Los sorteos del Quini 6 son eventos independientes (i.i.d.). Ningún análisis de datos históricos predice resultados futuros. El análisis es únicamente descriptivo y de entretenimiento.
      </div>
    </div>
  );
}
