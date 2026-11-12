# Lista de verificación previa a la demostración

Completar en orden 30 minutos antes. Marcar cada casilla al comprobarla.

## 1. Infraestructura

- [ ] Oracle en marcha: `docker ps` muestra `oracle-local` (puerto 1522).
- [ ] Redis en marcha: `docker ps` muestra `deploy-redis-1`; `docker exec deploy-redis-1 redis-cli PING` → `PONG`.
- [ ] Backend en marcha con el perfil correcto (`dev` para la demostración con SQL visible, o `prod` con las variables de entorno del README): `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.
- [ ] Flyway en V4: `select "version" from "flyway_schema_history"` muestra 1 a 4.
- [ ] Índice en uso: `EXPLAIN PLAN` de la página de productos muestra `IX_PRODUCTOS_NOMBRE_ID` (INC-02).
- [ ] SPA: Nginx `http://localhost:8081` (build de producción) o `ng serve` en `:4200` sin errores.

## 2. Datos de demostración

- [ ] Ejecutado `docs/sql/restablecer_demo.sql` y después `docker exec deploy-redis-1 redis-cli FLUSHALL`.
- [ ] Volumen cargado: 5 020 productos y más de 20 000 ventas (`docs/sql/volumen_pruebas.sql` solo si la base es nueva).
- [ ] Cliente conocido: **Rosa Mamani Condori**, DNI 71000003, activo.
- [ ] Productos conocidos: Loratadina 10mg (stock 45), Paracetamol 500mg (50), Amoxicilina 500mg (40, S/ 18.90); Jarabe para la tos sin stock y Naproxeno inactivo.
- [ ] Usuarios: `admin`, `vendedor`, `almacen` (contraseñas de prueba de `V2__seguridad.sql`); `inactivo` deshabilitado.
- [ ] El log no contiene datos personales: buscar el DNI `71000003` en el log del día → 0 coincidencias (INC-08).

## 3. Navegador y herramientas

- [ ] Sesiones cerradas en todas las pestañas; `sessionStorage` vacío (sin borrador de venta de un ensayo).
- [ ] Pestañas en el orden del guion: lámina, SPA, SPA en incógnito, terminal `pharma-frontend`, terminal `Practica` con el log, Postman.
- [ ] DevTools anclado abajo, pestaña **Red** con «Disable cache» desmarcado; zoom del navegador al 110 % para que se lea.
- [ ] Postman con la colección `docs/postman/PharmaBackend.postman_collection.json` importada y la carpeta «0. Sesión» ejecutada.
- [ ] `npx playwright test` ejecutado una vez antes (15 pruebas en verde) y vuelto a ejecutar `restablecer_demo.sql`.
- [ ] Notificaciones del sistema desactivadas; cargador conectado.

## 4. Plan B

- [ ] Capturas de cada bloque a mano (no se graba video: el docente indicó que no es necesario).
- [ ] Carpeta `docs/evidencias/s14-estabilizacion/` abierta en el explorador (capturas de cada bloque).
- [ ] PDF `Tineo_LP2_S14_Practica.pdf` e informe de la autónoma a mano.
- [ ] Si Redis cae: la aplicación sigue respondiendo (sesión 13); explicarlo y continuar.
- [ ] Si Oracle no levanta: mostrar las capturas del bloque que falló.
