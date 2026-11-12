# Guion de demostración · PharmaSoft (10 minutos)

Evaluación de la Unidad III, 19/11/2026. Antes de empezar, completar [`checklist-demo.md`](checklist-demo.md).

**Pestañas preparadas (en este orden):** 1) lámina `docs/arquitectura.png` · 2) SPA `http://localhost:8081/login` (Nginx; plan B `:4200`) · 3) SPA en ventana de incógnito para el segundo usuario · 4) terminal en `pharma-frontend` · 5) terminal en `Practica` con el log abierto · 6) Postman con la colección `PharmaBackend · S14`.

**Datos de la demostración:** cliente **Rosa Mamani Condori** (DNI 71000003) · producto **Loratadina 10mg** (stock 45 tras `restablecer_demo.sql`) · usuarios `vendedor`, `almacen` y `admin` (contraseñas de `V2__seguridad.sql`, solo desarrollo).

| Tiempo | Bloque | Qué hago | Qué digo (idea central) | Si falla (plan B) |
|---|---|---|---|---|
| 0:00–1:00 | **Contexto** | Muestro la lámina de arquitectura. | Botica con tres locales; vendedores, almacenero y administradora. SPA Angular servida por Nginx, API Spring Boot con JWT, Oracle como fuente de verdad y Redis como caché. Lo crítico es el flujo de venta. | La lámina está también en el PDF del informe. |
| 1:00–4:30 | **Flujo principal** | 1) Login como `vendedor`. 2) Nueva venta: busco «Mamani» y la elijo. 3) Busco «Lora» (búsqueda en el servidor) → Loratadina, cantidad 2; el total se calcula. 4) Revisar → Confirmar (sin imprimir). 5) Comprobante «Venta N.º … registrada». 6) Menú **Productos** (sin F5): Loratadina muestra 43. | La venta es una sola transacción: si falta stock no queda nada a medias. Al registrar se invalida la caché de productos, por eso el stock ya aparece descontado. | Capturas `I-03a/b/c`; E2E-07 hace exactamente este flujo. |
| 4:30–6:00 | **Seguridad** | 1) El menú del vendedor no tiene Reportes ni Auditoría. 2) Escribo `/auditoria` en la URL → «403 · Acceso denegado». 3) **401 por token vencido:** DevTools → Application → Session Storage → `pharmasoft.sesion`, altero el token y recargo → login con «Su sesión expiró» y `returnUrl`; al entrar vuelvo a la página. 4) Postman: «403 Vendedor consulta la bitácora». | El menú y los guards son comodidad; quien decide es el backend con `hasAuthority`. Un token vencido o alterado responde 401 y la SPA cierra la sesión recordando dónde estaba. | Capturas `I-07a/b/c`; pruebas E2E-13, E2E-14 y E2E-15. |
| 6:00–7:30 | **Volumen y rendimiento** | 1) Como `admin`, Productos: «1–20 de 5 020», «Siguiente» → `?pagina=1` en la URL; F5 conserva la página. 2) Terminal: `docker exec deploy-redis-1 redis-cli --scan --pattern "productos*"` y `TTL`. 3) Postman o navegador: `/actuator/health` (UP) y `/actuator/metrics/cache.gets` como admin. | Oracle devuelve solo 20 filas (`OFFSET … FETCH`) y un `COUNT`; el índice `(nombre, id)` evita ordenar toda la tabla. La primera página queda en Redis 10 minutos. | Capturas `A-02`, `A-08`, `A-10`, `A-11`. |
| 7:30–8:30 | **Auditoría** | 1) Auditoría con filtro `VENTA_REGISTRADA` y usuario `vendedor`: la venta del bloque 2 con su código. 2) En el terminal: `Select-String "[<código>]" logs\…` (o `evidencias-s14\dev.log`) → las líneas de esa venta. | La bitácora dice qué pasó y quién; el log dice cómo lo hizo el sistema; el `requestId` los une. El evento se escribe después del commit. | Capturas `T-01`, `T-02`, `T-03`. |
| 8:30–10:00 | **Calidad** | 1) Terminal: `npx playwright test` (15 pruebas, ~16 s) en vivo. 2) Mientras corre, abro `docs/incidencias.md`: 9 registradas, 7 cerradas, ninguna crítica ni alta abierta. 3) Cierro con `docs/resultados-pruebas.md`: 45/45 en tres corridas. | Una prueba inestable se corrige, no se reintenta (1 worker, `retries: 0`). Cada incidencia tiene causa, commit y prueba. | Captura `R-01` y el informe HTML (`R-02`). |

## Frases de cierre (15 s)

PharmaSoft registra ventas sin dejar datos a medias, protege cada operación por permisos, soporta el volumen de un año paginando en el servidor, deja rastro de quién hizo qué y lo demuestra con 15 pruebas que pasan una y otra vez.

## Ensayos cronometrados

| Ensayo | Fecha | Duración total | Bloque que se pasó de tiempo | Ajuste |
|---|---|---|---|---|
| 1 | *(por completar)* | | | |
| 2 | *(por completar)* | | | |

## Verificación manual del flujo principal

Ejecutar tres veces desde una base recién preparada (`docs/sql/restablecer_demo.sql` + `FLUSHALL`), siguiendo el bloque 2 del guion.

| Corrida | Fecha y hora | Duración del flujo | Venta N.º | Stock de Loratadina antes → después | Desvíos |
|---|---|---|---|---|---|
| 1 | *(por completar)* | | | | |
| 2 | *(por completar)* | | | | |
| 3 | *(por completar)* | | | | |

