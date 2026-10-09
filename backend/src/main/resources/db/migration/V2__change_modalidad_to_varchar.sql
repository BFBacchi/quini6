-- ══════════════════════════════════════════════════════════
-- Quini6 Analytics — Schema V2 (Flyway)
-- Cambia modalidad_enum a VARCHAR para compatibilidad con JPA @Enumerated(STRING)
-- ══════════════════════════════════════════════════════════

-- Eliminar constraint único primero
ALTER TABLE resultados DROP CONSTRAINT uq_resultado_sorteo_modalidad;

-- Cambiar tipo de columna en resultados
ALTER TABLE resultados ALTER COLUMN modalidad TYPE VARCHAR(50);

-- Cambiar tipo de columna en numeros_sorteados
ALTER TABLE numeros_sorteados ALTER COLUMN modalidad TYPE VARCHAR(50);

-- Volver a crear constraint único
ALTER TABLE resultados ADD CONSTRAINT uq_resultado_sorteo_modalidad UNIQUE (sorteo_id, modalidad);

-- Eliminar tipo enum ya no usado
DROP TYPE modalidad_enum;