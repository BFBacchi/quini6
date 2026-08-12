import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('q6_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('q6_token');
      localStorage.removeItem('q6_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// ─── API calls ──────────────────────────────────────────

export interface Sorteo {
  id: string;
  numeroSorteo: number;
  fecha: string;
  pozoMonto: number | null;
  resultados: Resultado[];
}

export interface Resultado {
  modalidad: string;
  numeros: number[];
  ganadores6: number | null;
  monto6: number | null;
  ganadores5: number | null;
  monto5: number | null;
  ganadores4: number | null;
  monto4: number | null;
}

export interface Frecuencia {
  totalSorteos: number;
  frecuencias: Record<number, {
    numero: number;
    absoluta: number;
    relativa: number;
    atraso: number;
  }>;
  disclaimer: string;
}

export interface Combinacion {
  numeros: number[];
  metodo: string;
  disclaimer: string;
}

export interface ChiCuadrado {
  chiCuadrado: number;
  gradosLibertad: number;
  pValue: number;
  totalSorteos: number;
  interpretacion: string;
}

export const sorteosApi = {
  listar: () => api.get<Sorteo[]>('/sorteos'),
  porNumero: (num: number) => api.get<Sorteo>(`/sorteos/${num}`),
  porRango: (desde: string, hasta: string) =>
    api.get<Sorteo[]>('/sorteos/rango', { params: { desde, hasta } }),
};

export const estadisticasApi = {
  frecuencia: (modalidad: string, desde?: string, hasta?: string) =>
    api.get<Frecuencia>('/estadisticas/frecuencia', {
      params: { modalidad, desde, hasta },
    }),
  chiCuadrado: (modalidad: string) =>
    api.get<ChiCuadrado>('/estadisticas/chi-cuadrado', {
      params: { modalidad },
    }),
};

export const generadorApi = {
  aleatorio: () => api.get<Combinacion>('/generador/aleatorio'),
  ponderado: (modalidad: string) =>
    api.get<Combinacion>('/generador/ponderado', { params: { modalidad } }),
};

export const authApi = {
  login: (username: string, password: string) =>
    api.post<{ token: string; username: string; role: string }>('/auth/login', {
      username,
      password,
    }),
};

export const adminApi = {
  ingerirSorteo: (numero: number) =>
    api.post(`/admin/ingesta/sorteo/${numero}`),
  ingerirRango: (desde: string, hasta: string) =>
    api.post('/admin/ingesta/rango', null, { params: { desde, hasta } }),
};

export interface MonitoringHealth {
  status: string;
  postgres: string;
  elasticsearch: string;
  rabbitmq: string;
}

export interface MonitoringDatabase {
  status: string;
  sorteos: number;
  resultados: number;
  usuarios: number;
  auditLogs: number;
  ultimoSorteo: number;
}

export interface MonitoringElasticsearch {
  status: string;
  cluster: Record<string, string>;
  indices: string | object[];
  error?: string;
}

export interface MonitoringRabbitMQ {
  status: string;
  overview: Record<string, string>;
  queues: string | object[];
  error?: string;
}

export const monitoringApi = {
  health: () => api.get<MonitoringHealth>('/monitoring/health'),
  database: () => api.get<MonitoringDatabase>('/monitoring/database'),
  elasticsearch: () => api.get<MonitoringElasticsearch>('/monitoring/elasticsearch'),
  rabbitmq: () => api.get<MonitoringRabbitMQ>('/monitoring/rabbitmq'),
  tableData: (table: string, page = 0, size = 100) =>
    api.get<TableDataResponse>(`/monitoring/database/${table}`, { params: { page, size } }),
  schema: () => api.get<SchemaResponse>('/monitoring/database/schema'),
};

export interface TableInfo {
  name: string;
  rows: number;
  schema: string;
}

export interface TableDataResponse {
  table: string;
  total: number;
  page: number;
  size: number;
  columns: string[];
  data: (string | number | boolean | null)[][];
}

export interface ColumnInfo {
  name: string;
  type: string;
  nullable: boolean;
  default: string | null;
  maxLength: number | null;
}

export interface ConstraintInfo {
  table: string;
  name: string;
  type: string;
  column: string;
  foreignTable?: string;
  foreignColumn?: string;
}

export interface SchemaResponse {
  tables: { table: string; columns: ColumnInfo[] }[];
  constraints: ConstraintInfo[];
  indexes: { table: string; name: string; definition: string }[];
}
