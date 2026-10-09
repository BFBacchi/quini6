-- ══════════════════════════════════════════════════════════
-- Quini6 Analytics — Schema V3 (Flyway)
-- Corrige el hash BCrypt del usuario admin seed (V1 tenía un hash inválido
-- que no correspondía a admin123, por lo que el login devolvía 401)
-- Password: admin123 (BCrypt, solo dev)
-- ══════════════════════════════════════════════════════════

UPDATE usuarios
SET password_hash = '$2b$10$C5CXoXQIZLPfkTTZgyDBBu.Q3G79DG/2os3KiwJp3ELTgVySPg81K',
    updated_at = now()
WHERE username = 'admin';