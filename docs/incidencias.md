# Registro de incidencias · sesiones 13 y 14

Ciclo seguido en cada una: reproducir → identificar (o escribir) la prueba que falla → corregir la causa raíz → ejecutar toda la suite → commit `fix(...)`/`perf(...)` con el ID.

Repositorios: **be** = PharmaBackend (`Legends600/TareaS1`), **fe** = pharma-frontend (`Legends600/pharma-frontend`). Severidad: crítica, alta, media o baja.

## Detalle

### INC-01 · Productos (SPA) · Media · Cerrada
- **Fecha:** 12/11/2026
- **Pasos:** con 5 020 productos cargados, admin → Productos → «Siguiente» → F5.
- **Esperado:** la URL guarda la página (`?pagina=1`) y F5 la conserva; el filtro de categoría considera todos los productos.
- **Obtenido:** 10 filas por página, la URL seguía en `/productos`, F5 volvía a la página 1 y el filtro de categoría solo filtraba la página visible (`A-05`, `A-06`).
- **Causa raíz:** página, tamaño y orden vivían en *signals* del componente y el filtro de categoría se aplicaba en el navegador.
- **Corrección:** `input()` desde la URL con `withComponentInputBinding`, `rxResource` + `linkedSignal` y filtro `categoriaId` en el servidor.
- **Commits:** fe `038a254`, be `3b7e589`.
- **Prueba que evita que vuelva:** E2E-06 (`paginacion.spec.ts`: «Siguiente» deja `pagina=1` y F5 la conserva) y E2E-11.

### INC-02 · Base de datos · Media · Cerrada
- **Fecha:** 12/11/2026
- **Pasos:** `EXPLAIN PLAN` de `select … from producto order by nombre, id offset 100 rows fetch first 20 rows only`.
- **Esperado:** que Oracle use el índice creado para ordenar por nombre.
- **Obtenido:** `TABLE ACCESS FULL` + `WINDOW SORT PUSHED RANK` sobre 5 020 filas, costo 16 (`A-04`).
- **Causa raíz:** la consulta ordena por `nombre, id` (desempate entre páginas) y el índice de V3 solo tenía `nombre`.
- **Corrección:** `V4__indice_productos_orden.sql` reemplaza el índice por `(nombre, id)` y el desempate usa el mismo sentido: `INDEX FULL SCAN` + `WINDOW NOSORT STOPKEY`, costo 4 (`A-08`). V3 ya estaba aplicada, por eso no se editó.
- **Commit:** be `d2a7b14`.
- **Prueba:** no automatizada: el plan depende de las estadísticas de la base y no del código. Se verifica con `EXPLAIN PLAN` en la lista de verificación previa a la demostración.

### INC-03 · Backend (listados) · Media · Cerrada
- **Fecha:** 12/11/2026
- **Pasos:** `GET /api/v1/ventas/buscar?desde=2026-11-01&hasta=2026-11-12` y contar en el log las líneas `org.hibernate.SQL` con su `requestId`.
- **Esperado:** un número de consultas fijo por página.
- **Obtenido:** 16 sentencias para 10 filas, 10 de ellas `select … from detalle_venta where venta_id=?` (N+1) (`A-03`).
- **Causa raíz:** las relaciones perezosas se cargaban fila por fila al convertir cada venta a DTO.
- **Corrección:** `hibernate.default_batch_fetch_size = 50` en `application.yaml`: 7 sentencias por página, `detalle_venta` en un solo `IN (…)` (`A-08`).
- **Commit:** be `f4c072b`.
- **Prueba:** no automatizada; se mide en el log por `requestId`. Podría cubrirse con las estadísticas de Hibernate en una prueba de integración.

### INC-04 · Ventas (nueva venta) · Alta · Cerrada
- **Fecha:** 12/11/2026
- **Pasos:** con el volumen cargado, vendedor → Nueva venta → buscar «Producto de prueba 04984».
- **Esperado:** el producto aparece si está activo y tiene stock.
- **Obtenido:** «Ningún producto activo con stock coincide»: no se podía vender (`A-07`).
- **Causa raíz:** la página descargaba los 100 primeros productos por nombre y filtraba solo esos en el navegador.
- **Corrección:** `GET /productos?nombre=&disponibles=true` en el servidor y búsqueda con 300 ms de espera en la SPA (`A-12`).
- **Commits:** be `3b7e589`, fe `038a254`.
- **Prueba:** E2E-07, E2E-08 y E2E-10 buscan productos por el servidor; Postman «200 Búsqueda de disponibles». No hay una prueba con un producto más allá del puesto 100 porque depende del volumen de carga, que no existe en una base recién preparada.

### INC-05 · Pruebas E2E · Baja · Cerrada
- **Fecha:** 12/11/2026
- **Pasos:** recorrer I-07 con tokens de 1 minuto y usar `iniciarSesion` mientras el aviso de expiración está visible.
- **Esperado:** el apoyo encuentra el botón «Cerrar sesión».
- **Obtenido:** *strict mode violation*: también existe «Cerrar sesión ahora».
- **Causa raíz:** localizador por subcadena.
- **Corrección:** `getByRole('button', { name: 'Cerrar sesión', exact: true })` en `e2e/apoyo.ts`.
- **Commit:** fe `8d64b0b`.
- **Prueba:** las 15 pruebas usan `iniciarSesion`; cualquier ambigüedad nueva hace fallar la suite.

### INC-06 · Ventas · Seguridad (I-07) · Media · Cerrada
- **Fecha:** 12/11/2026
- **Pasos:** vendedor en `/ventas/nueva` con cliente y producto elegidos; el token vence → login → volver a entrar.
- **Esperado:** volver a la venta sin perder lo ingresado.
- **Obtenido:** volvía a `/ventas/nueva`, pero vacía.
- **Causa raíz:** el formulario solo existía en memoria y se destruía al cerrar la sesión.
- **Corrección:** borrador en `sessionStorage`, restaurado solo para el mismo usuario y borrado al registrar o cancelar (`I-07c`).
- **Commit:** fe `bbe5290`.
- **Prueba:** E2E-15 (`sesion.spec.ts`).

### INC-07 · Ventas · Clientes · Baja · **Abierta**
- **Fecha:** 12/11/2026
- **Pasos:** registrar más de 100 clientes y buscar uno cuyo apellido quede después del puesto 100 en la nueva venta o en el filtro de Ventas.
- **Esperado:** aparece en el buscador.
- **Obtenido:** hoy hay 23 clientes y siempre aparece; con más de 100 no aparecería.
- **Causa raíz:** el mismo patrón de INC-04, aún no aplicado a clientes (`clienteService.listar(0, 100)`).
- **Corrección propuesta:** filtro de texto en `GET /clientes` y búsqueda en el servidor, igual que en productos.
- **Prueba:** E2E-10 cubre el caso actual (cliente recién registrado en el buscador).
- **Por qué sigue abierta:** con el volumen real de la botica (decenas de clientes) no se manifiesta; se prioriza después de la evaluación.

### INC-08 · Logs (S13) · Media · Cerrada
- **Fecha:** 05/11/2026
- **Pasos:** registrar un cliente y revisar el log; enviar un token inválido.
- **Esperado:** el log no contiene datos personales ni el contenido del token.
- **Obtenido:** el log mostraba el DNI del cliente y el mensaje completo de la excepción del token.
- **Causa raíz:** mensajes de log escritos sin revisar qué datos incluían.
- **Corrección:** el log de clientes ya no muestra el DNI y el del filtro JWT solo registra el tipo de error.
- **Commit:** be `2367c8d`.
- **Prueba:** no automatizada; la lista de verificación incluye buscar el DNI de prueba en el log.

### INC-09 · Errores de la API (S13) · Baja · **Abierta**
- **Fecha:** 05/11/2026
- **Pasos:** `GET /api/v1/auditoria?desde=12-11-2026` (fecha con formato inválido).
- **Esperado:** 400 con `message` y `requestId` en el cuerpo, como los demás errores.
- **Obtenido:** 400 con el cuerpo por defecto de Spring (`timestamp`, `status`, `error`, `path`); el `requestId` solo llega en la cabecera `X-Request-Id`.
- **Causa raíz:** `GlobalExceptionHandler` no maneja `MethodArgumentTypeMismatchException` ni el JSON mal formado.
- **Corrección propuesta:** agregar esos dos manejadores.
- **Prueba:** Postman «400 Fecha con formato inválido» (comprueba el código, no el cuerpo).
- **Por qué sigue abierta:** la SPA nunca envía fechas en ese formato (usa `<input type="date">`) y el código de seguimiento sí llega en la cabecera.

## Resumen

| ID | Fecha | Módulo | Severidad | Estado | Commit | Prueba |
|---|---|---|---|---|---|---|
| INC-01 | 12/11 | Productos (SPA) | Media | Cerrada | fe 038a254 · be 3b7e589 | E2E-06, E2E-11 |
| INC-02 | 12/11 | Base de datos | Media | Cerrada | be d2a7b14 | Manual (`EXPLAIN PLAN`) |
| INC-03 | 12/11 | Backend | Media | Cerrada | be f4c072b | Manual (log) |
| INC-04 | 12/11 | Ventas | **Alta** | Cerrada | be 3b7e589 · fe 038a254 | E2E-07, E2E-08, E2E-10, Postman |
| INC-05 | 12/11 | Pruebas E2E | Baja | Cerrada | fe 8d64b0b | Toda la suite |
| INC-06 | 12/11 | Ventas · Seguridad | Media | Cerrada | fe bbe5290 | E2E-15 |
| INC-07 | 12/11 | Ventas · Clientes | Baja | Abierta | — | E2E-10 (caso actual) |
| INC-08 | 05/11 | Logs | Media | Cerrada | be 2367c8d | Manual (lista de verificación) |
| INC-09 | 05/11 | Errores de la API | Baja | Abierta | — | Postman |

**Por severidad:** crítica 0 · alta 1 (cerrada) · media 5 (cerradas) · baja 3 (1 cerrada, 2 abiertas).

**Por módulo:** Ventas 3 (INC-04, INC-06, INC-07) · Productos 1 · Base de datos 1 · Backend/listados 1 · Pruebas E2E 1 · Logs 1 · Errores de la API 1.

**Abiertas: 2, ambas de severidad baja.** INC-07 no se manifiesta con el volumen actual de clientes; INC-09 no afecta a la SPA y el código de seguimiento llega en la cabecera. **No queda ninguna incidencia crítica ni alta abierta.**
