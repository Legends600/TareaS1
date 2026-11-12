-- =====================================================================
-- Restablece los datos de la demostración (solo desarrollo).
--
-- Las pruebas E2E y los ensayos bajan el stock y cambian el precio de
-- la amoxicilina. Este script deja los productos de la demostración en
-- valores conocidos. No borra ventas ni eventos: son historia.
-- Después de ejecutarlo vaciar la caché:
--   docker exec deploy-redis-1 redis-cli FLUSHALL
-- =====================================================================

update producto set stock = 50, estado = true  where nombre = 'Paracetamol 500mg';
update producto set stock = 50, estado = true  where nombre = 'Ibuprofeno 400mg';
update producto set stock = 45, estado = true  where nombre = 'Loratadina 10mg';
update producto set stock = 40, estado = true, precio = 18.90 where nombre = 'Amoxicilina 500mg';
update producto set stock = 90, estado = true  where nombre like 'Gasa est%';

-- Casos que la demostración muestra como «no se ofrecen en la venta»
update producto set stock = 0,  estado = true  where nombre = 'Jarabe para la tos 120ml';
update producto set estado = false             where nombre = 'Naproxeno 550mg';

-- Usuario de prueba deshabilitado (login 401)
update usuarios set estado = false where username = 'inactivo';

commit;

select nombre, stock, precio, estado from producto
where nombre like 'Gasa est%' or nombre in ('Paracetamol 500mg', 'Ibuprofeno 400mg', 'Loratadina 10mg', 'Amoxicilina 500mg',
                 'Jarabe para la tos 120ml', 'Naproxeno 550mg')
order by nombre;
