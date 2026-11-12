# Defensa técnica · diez preguntas del jurado

Rutas del backend relativas a `src/main/java/pe/edu/upeu/Practica/`; las de la SPA, al repositorio pharma-frontend.

## 1. ¿Por qué la paginación se hace en el servidor y no en la SPA? ¿Qué consultas ejecuta Oracle por cada página?

Con un año de datos hay más de 5 000 productos y 20 000 ventas. Si la SPA pidiera todo para paginar en el navegador, cada visita descargaría cientos de kilobytes, Oracle leería la tabla completa y el navegador tendría miles de filas en memoria; además, cualquier filtro que se aplicara en el navegador solo vería lo descargado (eso fue INC-04: la nueva venta no encontraba productos fuera de los 100 primeros). En el servidor, cada página trae solo 20 filas (unos 5 KB).

Por cada página Oracle ejecuta **dos consultas**: la de la página, `select … from producto p1_0 where … order by p1_0.nombre, p1_0.id offset ? rows fetch first ? rows only`, y la de conteo, `select count(p1_0.id) from producto p1_0 where …`, que da `totalElementos` y `totalPaginas`. Las relaciones (categoría) se cargan en lote con un `IN (…)` gracias a `default_batch_fetch_size` (INC-03). El orden lleva `id` como desempate para que una fila no salte entre páginas, y el índice `(nombre, id)` permite a Oracle recorrer el índice y detenerse al completar la página en lugar de ordenar toda la tabla (INC-02).

- `service/impl/ProductoServiceImpl.java` → `listarPaginado` (lista blanca de orden, tamaño 1–100, `Sort` con desempate).
- `repository/ProductoRepository.java` → `buscar` con `countQuery`.
- `util/PaginacionUtil.java` y `resources/db/migration/V4__indice_productos_orden.sql`.
- SPA: `features/productos/pages/producto-list/producto-list.ts` (página en la URL con `input()` y `rxResource`).

## 2. ¿Cómo garantizas que una venta no deja datos a medias y que su evento de auditoría no aparece si la venta falla?

`VentaServiceImpl.registrar` es `@Transactional`: cabecera, detalles y descuento de stock se confirman juntos o no se confirma nada. Si una regla falla (cliente inactivo, producto inactivo, stock insuficiente) se lanza `ReglaNegocioException`, Spring hace *rollback* y el `GlobalExceptionHandler` responde 409. El stock no se descuenta leyendo y escribiendo, sino con un `UPDATE` condicional: `set stock = stock - :cantidad where id = :id and stock >= :cantidad`; si otra caja vendió primero, actualiza 0 filas y la venta se revierte (E2E-08 lo prueba).

El servicio no escribe en la bitácora: publica un `EventoAuditoria` y `AuditoriaListener` lo recibe con `@TransactionalEventListener(phase = AFTER_COMMIT)`. Si hubo *rollback*, el evento se descarta. El evento se guarda en su propia transacción (`REQUIRES_NEW`) y, si eso falla, solo se escribe en el log: la venta ya confirmada no se convierte en error.

- `service/impl/VentaServiceImpl.java` → `registrar`.
- `repository/ProductoRepository.java` → `descontarStock`.
- `auditoria/AuditoriaListener.java`, `service/impl/AuditoriaServiceImpl.java` → `registrar`.
- Evidencia: `B-01` (venta con 409 no suma evento) y el orden del log en `T-02`.

## 3. ¿Qué ocurre, paso a paso, cuando el token vence a mitad de una venta?

1. Al iniciar sesión, `AuthService` guarda la sesión en `sessionStorage` con `expiraEn` y programa un temporizador; dos minutos antes el aviso muestra la cuenta regresiva.
2. Mientras tanto, la nueva venta guarda el borrador (cliente y líneas) en `sessionStorage`.
3. Al vencer, el temporizador llama a `cerrarSesion('expirada', router.url)`. Si una petición llega antes al backend con el token vencido o alterado, `JwtAuthenticationFilter` lo rechaza, `RestAuthenticationEntryPoint` responde 401 y `errorInterceptor` hace lo mismo.
4. `cerrarSesion` borra la sesión y navega a `/login?expirada=1&returnUrl=%2Fventas%2Fnueva`; el login muestra «Su sesión expiró».
5. Tras iniciar sesión, `Login.destino()` acepta solo rutas internas y vuelve a `/ventas/nueva`.
6. `VentaForm` restaura el borrador si pertenece al mismo usuario y muestra «Se recuperó la venta que estaba en curso» (INC-06). La venta no se registró: no hay nada a medias en Oracle.

- SPA: `core/auth/auth-service.ts` (`programarExpiracion`, `cerrarSesion`), `core/interceptors/error-interceptor.ts`, `features/auth/pages/login/login.ts`, `features/ventas/pages/venta-form/venta-form.ts` (`restaurarBorrador`).
- Backend: `security/JwtAuthenticationFilter.java`, `security/RestAuthenticationEntryPoint.java`.
- Pruebas: E2E-14 y E2E-15; capturas `I-07a/b/c`.

## 4. ¿Por qué la caché de productos se invalida al registrar una venta? ¿Qué pasaría si no?

La caché `productos` guarda páginas completas con el stock de cada producto. Una venta cambia el stock, así que esas páginas quedan desactualizadas. `registrar` y `anular` tienen `@CacheEvict(cacheNames = "productos", allEntries = true)`. Se borran todas las entradas porque una venta puede afectar a cualquier página y a cualquier orden, y la siguiente lectura vuelve a Oracle.

Si no se invalidara, durante el TTL (10 minutos) Productos mostraría el stock anterior a la venta: un vendedor podría ofrecer unidades que ya no existen y el almacenero vería datos falsos. El backend igual evitaría vender de más gracias al `UPDATE` condicional, pero la pantalla mentiría. E2E-07 lo verifica: lee el stock antes de vender y comprueba que Productos muestra el valor descontado sin F5. Las búsquedas por texto no se cachean (`condition`) para no llenar Redis con un término por tecla.

- `service/impl/VentaServiceImpl.java` (`@CacheEvict`), `service/impl/ProductoServiceImpl.java` (`@Cacheable` con `condition`), `config/CacheConfig.java`, `application.yaml` (TTL 10 min).

## 5. ¿Qué diferencia hay entre un log, una columna de auditoría y un evento de la bitácora?

- **Columna de auditoría** (`creado_por`, `modificado_por`): está en la misma fila y dice quién la creó y quién la cambió por última vez; no guarda historia. La llena Spring Data JPA con `AuditorAware`.
- **Evento de la bitácora** (`auditoria_eventos`): una fila por operación de negocio importante, con fecha, usuario, acción, entidad, id, detalle corto y `requestId`. Es historia consultable por el administrador.
- **Log**: describe qué hizo el sistema en cada petición (inicio, stock descontado, fin, errores, tiempos), para soporte técnico. Es texto en archivo con rotación, no un dato de negocio.

El `requestId` une los tres: está en el evento y en cada línea del log de esa petición (`T-02`). Ninguno guarda contraseñas, tokens ni el DNI de los clientes.

- `config/AuditoriaConfig.java`, `entity/AuditoriaEvento.java`, `config/RequestIdFilter.java`; detalle en [`auditoria.md`](auditoria.md).

## 6. ¿Cómo decides si una prueba end-to-end es estable? ¿Qué hiciste con una que fallaba a veces?

Es estable si pasa tres veces seguidas en la suite completa, con un solo worker y sin reintentos (`retries: 0`), sin `waitForTimeout` y sin depender de datos que otra prueba cambió. Para eso cada prueba inicia sesión por su cuenta, lee el estado antes de actuar (`stockAntes`) en lugar de suponer valores fijos, espera respuestas concretas (`waitForResponse`) y usa `data-testid` o roles accesibles. Resultado: 45/45 en tres corridas.

La que falló fue el apoyo `iniciarSesion` (INC-05): al recorrer I-07 con tokens de 1 minuto, el aviso de expiración agrega «Cerrar sesión ahora» y el localizador por subcadena encontraba dos botones. No la reintenté: la registré, encontré la causa en el mensaje de *strict mode* y en la traza, la corregí con `exact: true` y volví a correr toda la suite.

- `playwright.config.ts`, `e2e/apoyo.ts`, `e2e/flujo-venta.spec.ts`; resultados en [`resultados-pruebas.md`](resultados-pruebas.md).

## 7. ¿Cómo evitas que dos cajas vendan la misma última unidad?

No basta leer el stock y luego restarlo: entre la lectura y la escritura otra venta puede consumirlo. `descontarStock` hace la verificación y la resta en una sola sentencia (`… and p.stock >= :cantidad`), que Oracle ejecuta de forma atómica sobre la fila. Si devuelve 0 filas, la venta lanza 409 y toda la transacción se revierte. E2E-08 reproduce el caso: el vendedor pide todo el stock que ve, otra caja vende una unidad por la API y al confirmar la SPA muestra «Stock insuficiente» y el stock solo baja por la otra venta.

- `repository/ProductoRepository.java` → `descontarStock`; `service/impl/VentaServiceImpl.java`; `e2e/ventas-reglas.spec.ts`.

## 8. ¿Quién decide los permisos: la SPA o el backend?

El backend. La SPA oculta opciones del menú (`sidebar.ts` filtra por `tienePermiso`), protege rutas con `permisoGuard` y oculta botones con `*appSiPermiso`, pero eso es experiencia de usuario. La decisión está en `SecurityConfig` con `hasAuthority(...)` por ruta y método, y en `@PreAuthorize` para los reportes: aunque alguien llame a la API directamente, `/api/v1/auditoria` responde 403 a quien no tenga `USUARIOS_ADMINISTRAR` (Postman «403 Vendedor consulta la bitácora»). Los permisos de cada rol están en `PermisosPorRol` y un usuario con varios roles recibe la unión.

- `config/SecurityConfig.java`, `security/PermisosPorRol.java`; SPA: `core/guards/permiso-guard.ts`, `shared/directives/si-permiso.ts`.

## 9. ¿Por qué la migración del índice es una V4 nueva y no una edición de V3?

Flyway guarda en `flyway_schema_history` el *checksum* de cada migración aplicada. Si edito V3 después de aplicarla, Flyway detecta que no coincide y la aplicación no arranca; en otra base (la del servidor) V3 ya se habría aplicado con el contenido anterior. Las migraciones aplicadas son historia: el cambio se hace con una migración nueva, `V4__indice_productos_orden.sql`, que elimina el índice `(nombre)` y crea `(nombre, id)`. Así todas las bases llegan al mismo esquema siguiendo los mismos pasos.

- `resources/db/migration/V3__auditoria.sql`, `V4__indice_productos_orden.sql`; evidencia `A-04` (antes) y `A-08` (después).

## 10. ¿Cómo sigues un error que reporta un vendedor?

La SPA muestra el mensaje del backend con su **código de seguimiento** (el `requestId`). `RequestIdFilter` lo genera al entrar la petición, lo pone en el MDC (todas las líneas de log de esa petición lo llevan), lo devuelve en `X-Request-Id` y `GlobalExceptionHandler` lo incluye en el cuerpo del error. Con ese código busco en el log (`Select-String "[código]"`) y veo la petición completa: qué se intentó, qué SQL corrió, en qué paso falló y cuánto tardó. Si fue una operación de negocio confirmada, el mismo código está en la bitácora. Queda pendiente que los errores que no pasan por `GlobalExceptionHandler` (fecha mal formada, JSON inválido) lo incluyan también en el cuerpo (INC-09).

- `config/RequestIdFilter.java`, `exception/GlobalExceptionHandler.java`; SPA: `core/utils/http-error.ts` (`codigoSeguimiento`).
