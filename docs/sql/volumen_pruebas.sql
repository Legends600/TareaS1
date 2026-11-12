-- =====================================================================
-- Volumen de prueba para medir la paginacion (sesion 14, paso 2)
--
-- Solo en desarrollo o pruebas. Nunca en produccion.
-- No es una migracion de Flyway: se ejecuta a mano, despues de
-- V3__auditoria.sql (usa la columna creado_por).
--
-- Las ventas de carga no tienen detalle: sirven para medir el listado,
-- no para validar reportes. Tras ejecutarlo, limpiar la cache:
--   docker exec deploy-redis-1 redis-cli FLUSHALL
-- =====================================================================

insert into producto (nombre, precio, stock, estado, categoria_id,
                      fecha_creacion, creado_por)
select 'Producto de prueba ' || lpad(level, 5, '0'),
       round(dbms_random.value(1, 200), 2),
       trunc(dbms_random.value(0, 500)),
       true,
       (select min(id) from categorias),
       systimestamp,
       'carga'
from dual connect by level <= 5000;

insert into ventas (fecha, total, estado, cliente_id, creado_por)
select systimestamp - numtodsinterval(dbms_random.value(0, 365), 'DAY'),
       round(dbms_random.value(5, 500), 2),
       'REGISTRADA',
       (select min(id) from clientes),
       'carga'
from dual connect by level <= 20000;

commit;

-- Para retirar el volumen de prueba:
-- delete from ventas   where creado_por = 'carga';
-- delete from producto where creado_por = 'carga';
-- commit;
