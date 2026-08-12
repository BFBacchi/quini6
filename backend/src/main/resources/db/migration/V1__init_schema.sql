-- ══════════════════════════════════════════════════════════
-- Quini6 Analytics — Schema V1 (Flyway)
-- Fuente de verdad transaccional (PostgreSQL)
-- ══════════════════════════════════════════════════════════

-- Enum para modalidades del Quini 6
CREATE TYPE modalidad_enum AS ENUM (
    'TRADICIONAL',
    'SEGUNDA',
    'REVANCHA',
    'SIEMPRE_SALE',
    'POZO_EXTRA'
);

-- ─── Usuarios (auth JWT) ───────────────────────────────
CREATE TABLE usuarios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER'
                            CHECK (role IN ('ADMIN', 'USER')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ─── Sorteos (encabezado por día) ─────────────────────
-- Un sorteo = un día (miércoles o domingo), contiene resultados para 5 modalidades
CREATE TABLE sorteos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_sorteo   INTEGER      NOT NULL UNIQUE,
    fecha           DATE         NOT NULL,
    pozo_monto      NUMERIC(15,2),
    fuente_api      VARCHAR(50)  NOT NULL DEFAULT 'Q6R',
    raw_json        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_sorteos_fecha ON sorteos (fecha);
CREATE INDEX idx_sorteos_numero ON sorteos (numero_sorteo);

-- ─── Resultados por modalidad ─────────────────────────
-- Cada sorteo tiene hasta 5 filas (una por modalidad)
CREATE TABLE resultados (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sorteo_id       UUID           NOT NULL
                    REFERENCES sorteos(id) ON DELETE CASCADE,
    modalidad       modalidad_enum NOT NULL,
    -- 6 números sorteados (0-45)
    numero_1        SMALLINT       NOT NULL CHECK (numero_1 BETWEEN 0 AND 45),
    numero_2        SMALLINT       NOT NULL CHECK (numero_2 BETWEEN 0 AND 45),
    numero_3        SMALLINT       NOT NULL CHECK (numero_3 BETWEEN 0 AND 45),
    numero_4        SMALLINT       NOT NULL CHECK (numero_4 BETWEEN 0 AND 45),
    numero_5        SMALLINT       NOT NULL CHECK (numero_5 BETWEEN 0 AND 45),
    numero_6        SMALLINT       NOT NULL CHECK (numero_6 BETWEEN 0 AND 45),
    -- Premios
    premio_6_aciertos_ganadores   INTEGER,
    premio_6_aciertos_monto       NUMERIC(15,2),
    premio_5_aciertos_ganadores   INTEGER,
    premio_5_aciertos_monto       NUMERIC(15,2),
    premio_4_aciertos_ganadores   INTEGER,
    premio_4_aciertos_monto       NUMERIC(15,2),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_resultado_sorteo_modalidad UNIQUE (sorteo_id, modalidad)
);

CREATE INDEX idx_resultados_sorteo ON resultados (sorteo_id);
CREATE INDEX idx_resultados_modalidad ON resultados (modalidad);
CREATE INDEX idx_resultados_numeros ON resultados
    (numero_1, numero_2, numero_3, numero_4, numero_5, numero_6);

-- ─── Tabla aplanada para analytics ────────────────────
-- Cada fila = un número individual de un sorteo (6 filas por resultado)
-- Usada como staging para indexar en Elasticsearch
CREATE TABLE numeros_sorteados (
    id              BIGSERIAL PRIMARY KEY,
    resultado_id    UUID           NOT NULL
                    REFERENCES resultados(id) ON DELETE CASCADE,
    sorteo_id       UUID           NOT NULL
                    REFERENCES sorteos(id) ON DELETE CASCADE,
    modalidad       modalidad_enum NOT NULL,
    numero          SMALLINT       NOT NULL CHECK (numero BETWEEN 0 AND 45),
    posicion        SMALLINT       NOT NULL CHECK (posicion BETWEEN 1 AND 6),
    fecha           DATE           NOT NULL,
    numero_sorteo   INTEGER        NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_numero_posicion_resultado UNIQUE (resultado_id, posicion)
);

CREATE INDEX idx_ns_fecha ON numeros_sorteados (fecha);
CREATE INDEX idx_ns_numero ON numeros_sorteados (numero);
CREATE INDEX idx_ns_modalidad ON numeros_sorteados (modalidad);
CREATE INDEX idx_ns_numero_fecha ON numeros_sorteados (numero, fecha);
CREATE INDEX idx_ns_sorteo ON numeros_sorteados (sorteo_id);

-- ─── Audit Log ────────────────────────────────────────
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    entidad         VARCHAR(100) NOT NULL,
    accion          VARCHAR(50)  NOT NULL,
    actor           VARCHAR(100) NOT NULL,
    timestamp       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    entidad_id      UUID,
    detalle_json    JSONB,
    diff_json       JSONB
);

CREATE INDEX idx_audit_timestamp ON audit_log (timestamp DESC);
CREATE INDEX idx_audit_entidad ON audit_log (entidad, entidad_id);

-- ─── Trigger para updated_at automático ───────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_sorteos_updated_at
    BEFORE UPDATE ON sorteos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ─── Usuario admin por defecto (solo dev) ─────────────
-- Password: admin123 (BCrypt)
INSERT INTO usuarios (username, password_hash, role) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');
