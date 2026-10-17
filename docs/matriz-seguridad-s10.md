# Matriz de pruebas de seguridad – S10 (Reto 02, Parte A)

- Alumno: Tineo · Proyecto: PharmaBackend (`Practica`) · Rama: `feature/seguridad-pruebas-tineo`
- Ejecución: 16/10/2026, instancia propia en `http://localhost:8081` (Spring Boot 4, Spring Security 7, JJWT 0.12.6, Oracle en Docker).
- Herramienta: Swagger UI (`/swagger-ui/index.html`), Try it out. Cada captura lleva arriba una banda con ID, caso, usuario/token y fecha y hora. Las verificaciones en la base (U-02) son salida real de `sqlplus`; C-01 es el registro de red del navegador.
- Resumen: **20 casos ejecutados (16 base + 4 propios), 20 pasan, 0 fallan.** Los 4 propios son A-05 (Autenticación), T-06 (Token), P-05 (Permisos) y U-03 (Usuarios), uno por grupo. Los casos con varias peticiones (A-01, A-02, P-01…) tienen una captura por petición.

Los mensajes son los del campo `message` del cuerpo JSON. Los tokens que aparecen en las capturas son de desarrollo y expiran a los 60 minutos.

| ID | Grupo | Usuario / token | Petición | Resultado esperado (código y mensaje) | Resultado obtenido | Estado | Evidencia |
|----|-------|-----------------|----------|---------------------------------------|--------------------|--------|-----------|
| A-01 | Autenticación | Sin token (login de admin, vendedor, almacen, supervisor) | `POST /api/v1/auth/login` con `{username, password}` de cada uno | 200; cada respuesta trae sus roles y permisos; supervisor = unión de VENDEDOR y ALMACENERO | 200 en los cuatro. admin: roles `[ADMIN]`, incluye `USUARIOS_ADMINISTRAR` y `REPORTES_VER`. vendedor: `[VENDEDOR]`, permisos `CATALOGO_LEER, CLIENTES_ESCRIBIR, CLIENTES_LEER, VENTAS_LEER, VENTAS_REGISTRAR`. almacen: `[ALMACENERO]`, `CATALOGO_ESCRIBIR, CATALOGO_LEER`. supervisor: `[ALMACENERO, VENDEDOR]`, `CATALOGO_ESCRIBIR, CATALOGO_LEER, CLIENTES_ESCRIBIR, CLIENTES_LEER, VENTAS_LEER, VENTAS_REGISTRAR` (la unión) | Pasa | `A-01-admin_swagger.png`, `A-01-vendedor_swagger.png`, `A-01-almacen_swagger.png`, `A-01-supervisor_swagger.png` |
| A-02 | Autenticación | Sin token | `POST /auth/login` con usuario `nadie`; luego `admin` con contraseña `Incorrecta2026` | 401 con el mismo mensaje en ambos casos | 401 «Usuario o contraseña incorrectos» en ambos (mensaje idéntico) | Pasa | `A-02-inexistente_swagger.png`, `A-02-password_swagger.png` |
| A-03 | Autenticación | Sin token | `POST /auth/login` de `inactivo` con su contraseña correcta (`Inactivo2026*`) | 401; el mensaje no revela que la cuenta existe | 401 «Usuario o contraseña incorrectos» (igual que A-02) | Pasa | `A-03_swagger.png` |
| A-04 | Autenticación | Sin token | `POST /auth/login` con `VENDEDOR` / `Vendedor2026*` | Regla decidida: el usuario **no distingue mayúsculas**, por lo tanto 200 | 200; `username` devuelto `vendedor` (justificación más abajo) | Pasa | `A-04_swagger.png` |
| A-05 *(propio)* | Autenticación | Sin token | `POST /auth/login` con `{"username":"admin"}` (sin password) | 400 con error de validación de `password` | 400 «Existen errores de validación»; `validationErrors.password` = «La contraseña es obligatoria» | Pasa | `A-05_swagger.png` |
| T-01 | Token | vendedor (token válido enviado sin la palabra Bearer) | `GET /auth/me` con `Authorization: <token>` | 401 «Debe iniciar sesión…» | 401 «Debe iniciar sesión para acceder a este recurso» | Pasa | `T-01_swagger.png` |
| T-02 | Token | vendedor (token con 1 carácter cambiado en el payload) | `GET /auth/me` con `Bearer <token alterado>` | 401 «El token no es válido» | 401 «El token no es válido» | Pasa | `T-02_swagger.png` |
| T-03 | Token | vendedor (token emitido con el secreto normal) | Instancia reiniciada con otro `app.jwt.secret`; `GET /auth/me` con el token anterior | 401 «El token no es válido» | 401 «El token no es válido». Después se reinició con el secreto normal | Pasa | `T-03_swagger.png` |
| T-04 | Token | vendedor (token emitido con `expiracion-minutos=1`) | Instancia con `--app.jwt.expiracion-minutos=1`; login (`expiraEnSegundos` = 60); `GET /auth/me` da 200 de inmediato y se repite 75 s después | 401 «El token ha expirado…» | 401 «El token ha expirado. Inicie sesión nuevamente». Después se reinició con la expiración normal (60) | Pasa | `T-04_swagger.png` |
| T-05 | Token | vendedor (token real del login) | Decodificar el payload (Base64URL) del token | `sub`, `authorities`, `iat`, `exp` correctos; sin datos sensibles | Header `{"alg":"HS384"}`; payload con solo `sub=vendedor`, `authorities` (6 entradas, las del vendedor), `iat`, `exp`; `exp - iat` = 3600 s; sin contraseña, hash, correo ni DNI | Pasa | `T-05_payload.png` |
| T-06 *(propio)* | Token | Token forjado con `alg: none` y sin firma, que se hace pasar por admin | `GET /usuarios` con `Bearer <header>.<payload>.` | 401 (un token sin firma no se acepta) | 401 «El token no es válido» | Pasa | `T-06_swagger.png` |
| P-01 | Permisos | vendedor | `POST /categorias`, `PUT /categorias/64` y `DELETE /categorias/64` | 403 en los tres | 403 «No tiene permisos para realizar esta operación» en los tres | Pasa | `P-01-post_swagger.png`, `P-01-put_swagger.png`, `P-01-delete_swagger.png` |
| P-02 | Permisos | almacen | `GET /clientes` y `POST /ventas` | 403 en ambos | 403 «No tiene permisos para realizar esta operación» en ambos | Pasa | `P-02-clientes_swagger.png`, `P-02-ventas_swagger.png` |
| P-03 | Permisos | vendedor y almacen | `GET /reportes/productos-mas-vendidos` | 403 (rechazo por `@PreAuthorize`) | 403 «No tiene permisos para realizar esta operación» con ambos usuarios | Pasa | `P-03-vendedor_swagger.png`, `P-03-almacen_swagger.png` |
| P-04 | Permisos | admin | `GET` de categorías, productos, clientes, ventas, reportes (`ventas-por-categoria`) y usuarios | 2xx en todas | 200 en las seis | Pasa | `P-04-categorias_swagger.png`, `P-04-productos_swagger.png`, `P-04-clientes_swagger.png`, `P-04-ventas_swagger.png`, `P-04-reportes_swagger.png`, `P-04-usuarios_swagger.png` |
| P-05 *(propio)* | Permisos | vendedor | `GET /usuarios` | 403 (el módulo de usuarios exige `USUARIOS_ADMINISTRAR`) | 403 «No tiene permisos para realizar esta operación» | Pasa | `P-05_swagger.png` |
| U-01 | Usuarios | admin | `POST /usuarios` con password `abcdefgh`; luego con `rolIds: []` | 400 en ambos (número obligatorio; al menos un rol) | 400 en ambos: `password` «La contraseña debe contener al menos una letra y un número»; `rolIds` «Debe asignar al menos un rol» | Pasa | `U-01-sinnumero_swagger.png`, `U-01-sinroles_swagger.png` |
| U-02 | Usuarios | admin | `POST /usuarios` para un empleado con cuenta (María, id 6, cuenta `mhuaman`); luego se borró `mhuaman` de la base y se recreó por la API (`mhuaman` / `Cajera2026`, rol VENDEDOR) | 409 (relación 1 a 1) y luego 201; la respuesta no trae contraseña; `password` guarda `$2a$10$…`; fila nueva en `usuario_roles` | 409 «El empleado Maria Huaman Torres ya tiene un usuario». Tras borrar `usuario_roles` y `usuarios` de `mhuaman`: 201 con `id` 21, roles `[VENDEDOR]`, sin campo de contraseña. Oracle: `password` empieza con `$2a$10$`, longitud 60; `usuario_roles` tiene (21, 2 = VENDEDOR) | Pasa | `U-02-conCuenta_swagger.png`, `U-02-alta_swagger.png`, `U-02_oracle.png` |
| U-03 *(propio)* | Usuarios | admin | `POST /usuarios` con username `vendedor` (ya existe) para María, que en ese momento no tenía cuenta | 409 por nombre de usuario repetido | 409 «Ya existe un usuario con el nombre: vendedor» | Pasa | `U-03_swagger.png` |
| C-01 | CORS | vendedor (token), desde `http://localhost:4200` | `fetch` a `http://localhost:8081/api/v1/productos` con `Authorization: Bearer <token>`, observando la red | La verificación (OPTIONS) responde 200 y la petición real 200 o 401, nunca un error de CORS | `OPTIONS` (preflight, `Origin: http://localhost:4200`) 200; `GET` real 200 (10 productos); sin errores de CORS en consola | Pasa | `C-01_red.png` |

## Justificación de A-04 (usuario en mayúsculas)

La API acepta `VENDEDOR` porque `UsuarioRepository` busca sin distinguir mayúsculas, el alta guarda el nombre en minúsculas (`trim().toLowerCase`) y el índice único de la base es sobre `upper(username)`. **Es la regla correcta**: en un sistema de personas, `Vendedor` y `vendedor` no deben ser dos cuentas distintas (evita suplantaciones por parecido y errores al teclear). Lo que sí se distingue es la contraseña. La respuesta devuelve el `username` normalizado y el token usa `sub=vendedor`.

## Datos de prueba modificados y restaurados

- **mhuaman (U-02):** se borraron sus filas de `usuario_roles` y `usuarios` y se recreó por la API con la misma contraseña (`Cajera2026`), rol VENDEDOR y estado activo. Quedó con otro `id` (antes 8, ahora 21). Su login responde 200 después de la prueba.
- **Secreto JWT (T-03):** la instancia 8081 se reinició con un secreto aleatorio distinto y luego con el normal del `application.yml`. No se modificó ningún archivo del proyecto.
- **Expiración (T-04):** se reinició con `--app.jwt.expiracion-minutos=1` y luego con el valor normal (60).
- No se tocaron roles ni estados de los demás usuarios. La instancia del puerto 8080 del alumno no se detuvo. Al terminar se cerraron solo los procesos de 8081 y 4200.

## Parte B (B-01 a B-08)

_Pendiente: la completarán los días 2 y 3 (cambio de contraseña, asignación de roles y activar/desactivar usuarios)._

| ID | Grupo | Usuario / token | Petición | Resultado esperado (código y mensaje) | Resultado obtenido | Estado | Evidencia |
|----|-------|-----------------|----------|---------------------------------------|--------------------|--------|-----------|
