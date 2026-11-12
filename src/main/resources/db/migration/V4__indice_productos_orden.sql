-- =====================================================================
-- V4: indice que de verdad sirve para la lista de productos
--
-- La lista se ordena por "nombre, id" (id desempata entre paginas).
-- ix_productos_nombre (V3) solo tiene nombre: Oracle no lo usa para ese
-- ORDER BY y ordena toda la tabla en cada pagina (TABLE ACCESS FULL +
-- WINDOW SORT). Con (nombre, id) recorre el indice en orden y se detiene
-- al completar la pagina (INDEX FULL SCAN + WINDOW NOSORT STOPKEY).
--
-- V3 ya esta aplicada: no se edita, se corrige con esta migracion.
-- =====================================================================

drop index ix_productos_nombre;

create index ix_productos_nombre_id on producto (nombre, id);
