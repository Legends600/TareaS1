# Auditoría de PharmaSoft

## Qué se audita y por qué

La botica necesita poder responder tres preguntas sin depender de la memoria de nadie:

1. **¿Quién creó o cambió este dato?** Por ejemplo, quién cambió el precio de la amoxicilina o quién registró a un cliente.
2. **¿Qué operaciones importantes ocurrieron, cuándo y por quién?** Ventas, anulaciones, cambios de precio, bajas, activación de usuarios e intentos de inicio de sesión.
3. **¿Qué pasó exactamente en una petición concreta?** Para investigar un error que reporta un vendedor con su código de seguimiento.

Para eso PharmaSoft combina tres mecanismos distintos, cada uno con un propósito diferente.

## Columnas de auditoría, bitácora y logs

| | Columnas de auditoría | Bitácora (`auditoria_eventos`) | Logs de la aplicación |
|---|---|---|---|
| Qué responde | Quién creó y quién modificó **por última vez** cada fila | **Qué operación** de negocio ocurrió, quién la hizo y cuándo | **Qué hizo el sistema** en cada petición, paso a paso |
| Dónde vive | `creado_por` / `modificado_por` en `categorias`, `producto`, `clientes` y `creado_por` en `ventas` | Tabla `auditoria_eventos` (V3), una fila por evento | Consola y archivo con rotación (`logs/` en el perfil `prod`) |
| Quién lo llena | Spring Data JPA con `AuditorAware`: el usuario del token o `sistema` | `AuditoriaListener` después del commit, `AuthServiceImpl` en el login | SLF4J/Logback en cada capa |
| Historia | No: solo el último valor | Sí: cada evento queda | Sí, mientras el archivo exista (rotación) |
| Quién lo consulta | Cualquiera que vea la entidad («Modificado por …» en la SPA) | Solo `USUARIOS_ADMINISTRAR` (`GET /api/v1/auditoria` y página Auditoría) | Soporte técnico en el servidor |
| Ejemplo | `producto.modificado_por = admin` | `PRECIO_MODIFICADO · admin · Amoxicilina 500mg: 19.5 -> 18.9` | `[90529cee] VentaServiceImpl - Fin registrar venta | ventaId=20088` |

El elemento que los une es el **`requestId`**: `RequestIdFilter` lo genera al entrar cada petición, lo pone en el MDC del log, lo devuelve en la cabecera `X-Request-Id` y en los errores, y la bitácora lo guarda en `request_id`. Con él se pasa de un evento de la bitácora a todas las líneas del log de esa petición.

Código:

- Columnas: `config/AuditoriaConfig.java` (`@EnableJpaAuditing`, `AuditorAware`) y las entidades `entity/Producto.java`, `Categoria.java`, `Cliente.java`, `Venta.java` (`@CreatedBy`, `@LastModifiedBy`, `@EntityListeners(AuditingEntityListener.class)`).
- Bitácora: `auditoria/EventoAuditoria.java`, `auditoria/AuditoriaListener.java`, `service/impl/AuditoriaServiceImpl.java`, `entity/AuditoriaEvento.java`, `repository/AuditoriaEventoRepository.java`, `controller/AuditoriaController.java`.
- Migración: `src/main/resources/db/migration/V3__auditoria.sql`.
- Logs: `config/RequestIdFilter.java` y `application-prod.yaml`.
- SPA (pharma-frontend): `src/app/features/auditoria/` y la ruta `/auditoria` con `permisoGuard` y `USUARIOS_ADMINISTRAR` en `app.routes.ts`; la opción del menú se filtra por permiso en `layout/sidebar/sidebar.ts`.

## Eventos de la bitácora

| Acción | Cuándo se registra | Entidad / id | Qué guarda en `detalle` | Desde |
|---|---|---|---|---|
| `VENTA_REGISTRADA` | Después del commit de `VentaServiceImpl.registrar` | Venta / id de la venta | `total=… \| items=…` | S14 práctica |
| `VENTA_ANULADA` | Después del commit de `VentaServiceImpl.anular` | Venta / id | `total=…` | S14 práctica |
| `PRECIO_MODIFICADO` | Después del commit de `ProductoServiceImpl.update`, solo si el precio cambió | Producto / id | `nombre: precioAnterior -> precioNuevo` | S14 práctica |
| `PRODUCTO_ELIMINADO` | Después del commit de la baja lógica | Producto / id | Nombre del producto | S14 práctica |
| `CLIENTE_REGISTRADO` | Después del commit de `ClienteServiceImpl.create` | Cliente / id | Vacío (sin DNI ni correo) | S14 autónoma |
| `CLIENTE_DESACTIVADO` | Después del commit de la baja lógica del cliente | Cliente / id | Vacío | S14 autónoma |
| `CATEGORIA_ELIMINADA` | Después del commit de `CategoriaServiceImpl.delete` | Categoria / id | Nombre de la categoría | S14 autónoma |
| `USUARIO_ACTIVADO` / `USUARIO_DESACTIVADO` | Después del commit de `UsuarioServiceImpl.cambiarEstado` | Usuario / id | `username=…` | S14 autónoma |
| `LOGIN_EXITOSO` | En `AuthServiceImpl.login`, tras emitir el token | Usuario / — | Vacío; el usuario va en `usuario` | S14 práctica |
| `LOGIN_FALLIDO` | En `AuthServiceImpl.login`, al capturar la `AuthenticationException` | Usuario / — | Vacío; nunca la contraseña | S14 práctica |

Todas las filas guardan además `fecha`, `usuario` (el del token, o el nombre que se intentó usar en el login) y `request_id`.

## Por qué los eventos de negocio se registran después del commit

Los servicios no escriben en la bitácora: **publican** un `EventoAuditoria` con `ApplicationEventPublisher` dentro de la transacción. `AuditoriaListener` lo recibe con `@TransactionalEventListener(phase = AFTER_COMMIT)`, es decir, solo si la transacción de la venta se confirmó. Si la venta hace *rollback* (stock insuficiente, cliente inactivo, error de base de datos), el evento se descarta y la bitácora no muestra una venta que no existe.

`AuditoriaServiceImpl.registrar` usa `Propagation.REQUIRES_NEW`: después del commit ya no hay una transacción activa que reutilizar, así que el evento se guarda en una propia. Si guardar el evento falla, `AuditoriaListener` lo escribe en el log con `log.error` pero **no** convierte en error una venta que ya se guardó.

Evidencia: en `B-01` una venta de 99 999 unidades responde 409 y el total de `VENTA_REGISTRADA` no cambia; en `N-01` la eliminación de una categoría con productos responde 409 y no aparece `CATEGORIA_ELIMINADA`.

## Por qué el login fallido no espera un commit

Un login fallido **termina en error** (`AuthenticationException` → 401). Si se publicara como evento de transacción, el listener `AFTER_COMMIT` nunca se ejecutaría y justamente los intentos fallidos —los que más interesan para detectar un ataque de fuerza bruta— no quedarían registrados. Por eso `AuthServiceImpl.login` llama directamente a `auditoriaService.registrar(...)`, que abre su propia transacción (`REQUIRES_NEW`) y se confirma aunque la operación de login falle. `LOGIN_EXITOSO` usa la misma vía por consistencia.

## Qué datos no se guardan nunca

| Dato | Por qué | Dónde se cuida |
|---|---|---|
| Contraseñas, ni en texto ni en hash | Una bitácora o un log no deben permitir reconstruir credenciales | `EventoAuditoria.login` solo recibe el `username`; `AuthServiceImpl` no escribe la contraseña |
| Tokens JWT | Quien lea la bitácora o el log podría suplantar al usuario | Ningún evento ni línea de log recibe el token |
| DNI, correo y teléfono de clientes | Son datos personales (Ley 29733); para la trazabilidad basta el id | `CLIENTE_REGISTRADO` deja el detalle vacío; el log del registro de clientes no muestra el DNI (INC-08) |
| Cuerpos completos de petición o respuesta | Pueden contener cualquiera de los anteriores | El detalle es un resumen corto (máximo 500 caracteres, `EventoAuditoria.recortar`) |

El usuario de un login fallido se recorta a 50 caracteres para que un valor malicioso muy largo no rompa la inserción.

## Consulta de la bitácora

- `GET /api/v1/auditoria?usuario=&accion=&desde=&hasta=&pagina=&tamanio=`: paginado, ordenado por fecha descendente, tamaño máximo 100 (409 si se excede); solo `USUARIOS_ADMINISTRAR` (403 para vendedor y almacenero).
- Página **Auditoría** de la SPA: filtros por usuario, acción y rango de fechas en la URL (`/auditoria?accion=VENTA_REGISTRADA&usuario=vendedor&pagina=1`) y el paginador compartido; columnas fecha, usuario, acción, entidad, id, detalle y código de seguimiento.

## Caso trazado de punta a punta: venta 20088

La vendedora registra desde la SPA una venta de 2 Loratadina 10mg a Rosa Mamani Condori (12/11/2026 10:14:42).

| Paso | Qué se ve | Evidencia |
|---|---|---|
| 1. Venta registrada en la SPA | Comprobante N.º 020088 y «Venta N.º 020088 registrada correctamente» | `evidencias/s14-estabilizacion/autonoma/T-01_venta-de-la-demostracion.png` |
| 2. Fila en `ventas` | `id 20088 · total 25 · REGISTRADA · creado_por = vendedor` | `T-02_traza-bd-bitacora-log.png` |
| 3. Evento en la bitácora | `id 276 · vendedor · VENTA_REGISTRADA · entidad_id 20088 · total=25.0 \| items=1 · request_id 90529cee` | `T-02` y `T-03_evento-en-la-pagina-auditoria.png` |
| 4. Log con el mismo `requestId` | `[90529cee]` Inicio registrar venta → Stock descontado (productoId=57, cantidad=2) → Fin registrar venta → **Auditoría VENTA_REGISTRADA** → `POST /api/v1/ventas -> 201 (24 ms)` | `T-02` |

El orden del log confirma el diseño: «Fin registrar venta» (10:14:42.432) es anterior a «Auditoría VENTA_REGISTRADA» (10:14:42.438), es decir, el evento se escribió **después** del commit de la venta.
