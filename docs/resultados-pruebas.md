# Resultados de las pruebas · sesión 14

## Pruebas end-to-end (Playwright)

| Dato | Valor |
|---|---|
| Fecha | 12/11/2026, 10:17 |
| Versión | pharma-frontend `43c4511` · PharmaBackend `4e62782` (rama `feature/integracion-estabilizacion-tineo`) |
| Entorno | Backend perfil `dev` con 5 020 productos y 20 000 ventas de carga, Oracle 23 y Redis 7 en Docker, `ng serve` en :4200, Chromium de Playwright 1.65 |
| Comando | `npx playwright test --repeat-each=3 --reporter=list,html` |
| Configuración | 1 worker, `retries: 0`, sin `waitForTimeout` |
| Pruebas | 15 × 3 corridas = **45 ejecutadas** |
| Aprobadas | **45** (15 / 15 / 15) |
| Fallidas | **0** · inestables (*flaky*): **0** |
| Duración total | **48,1 s** |
| Informe | `playwright-report/index.html` (local) · captura `evidencias/s14-estabilizacion/autonoma/R-02_informe-html-15x3.png` |
| Consola | `evidencias/s14-estabilizacion/autonoma/R-01_playwright-15x3.png` |

| ID | Archivo | Prueba | Qué demuestra |
|---|---|---|---|
| E2E-01 | `autenticacion.spec.ts` | Ruta protegida sin sesión | Lleva al login con `returnUrl` |
| E2E-02 | `autenticacion.spec.ts` | Credenciales incorrectas | Muestra el mensaje del backend (401) |
| E2E-03 | `autenticacion.spec.ts` | Volver a la página pedida | Tras iniciar sesión regresa a `/ventas` |
| E2E-04 | `permisos.spec.ts` | Almacenero sin Ventas | Sin opción en el menú y URL denegada |
| E2E-05 | `permisos.spec.ts` | Vendedor en Productos | Ve la tabla sin botones de edición |
| E2E-06 | `paginacion.spec.ts` | Paginación en el servidor | 20 filas, `pagina=1` en la URL, F5 la conserva |
| E2E-07 | `flujo-venta.spec.ts` | Flujo principal | Venta 201 y Productos muestra el stock descontado (caché invalidada) |
| E2E-08 | `ventas-reglas.spec.ts` | Más unidades que el stock | Otra caja vende antes; la SPA muestra el 409 y la venta no se registra |
| E2E-09 | `ventas-reglas.spec.ts` | Venta sin productos | No se puede revisar ni registrar (0 POST) |
| E2E-10 | `integracion.spec.ts` | Cliente nuevo y venta inmediata | Integración Clientes → Venta (I-01) |
| E2E-11 | `integracion.spec.ts` | Filtro desde la página 3 | Al cambiar la fecha vuelve a la página 1 (I-08) |
| E2E-12 | `auditoria.spec.ts` | Bitácora como admin | La venta aparece con su id y su `requestId` |
| E2E-13 | `auditoria.spec.ts` | Auditoría como vendedor | Sin opción en el menú y URL denegada |
| E2E-14 | `sesion.spec.ts` | Token alterado | 401 → login con «Su sesión expiró» y `returnUrl` |
| E2E-15 | `sesion.spec.ts` | Venta en curso tras un 401 | Tras volver a entrar, la venta se recupera (INC-06) |

Historial de corridas `--repeat-each=3`:

| Fecha | Versión fe | Pruebas | Resultado | Duración |
|---|---|---|---|---|
| 12/11/2026 09:43 | `bbe5290` (práctica) | 7 × 3 = 21 | 21 aprobadas | 20,0 s |
| 12/11/2026 10:10 | `073af56` | 14 × 3 = 42 | 42 aprobadas | 45,5 s |
| 12/11/2026 10:17 | `43c4511` | 15 × 3 = 45 | 45 aprobadas | 48,1 s |

Ninguna prueba falló de forma intermitente. El único defecto de la suite (INC-05, localizador ambiguo) se detectó al recorrer la matriz con tokens de 1 minuto y se corrigió antes de estas corridas.

## Colección de Postman

| Dato | Valor |
|---|---|
| Archivo | `docs/postman/PharmaBackend.postman_collection.json` |
| Ejecución | `npx newman run docs/postman/PharmaBackend.postman_collection.json` (12/11/2026, contra `4e62782`) |
| Peticiones | **28**, todas con aserciones |
| Aserciones | **33 aprobadas, 0 fallidas** |
| Duración | 2,7 s · tiempo medio de respuesta 18 ms |
| Captura | `evidencias/s14-estabilizacion/autonoma/R-03_postman-newman.png` |

| Código | Productos | Ventas | Auditoría |
|---|---|---|---|
| 200/201 | Página 0 de 20 · búsqueda de disponibles | 201 registrar venta · 200 buscar por rango | La venta en la bitácora (mismo id y `requestId`) · login fallido registrado |
| 400 | Producto sin nombre | Venta sin productos | Fecha con formato inválido |
| 401 | Sin token | Sin token | Sin token |
| 403 | Vendedor crea un producto | Almacenero registra una venta | Vendedor consulta la bitácora |
| 404 | Producto inexistente | Venta inexistente | Ruta inexistente |
| 409 | Tamaño 500 · orden fuera de la lista blanca | Stock insuficiente · rango invertido | Tamaño 500 · rango invertido |

La carpeta «0. Sesión» obtiene los tokens de admin, vendedor y almacenero y registra un login fallido; los tokens se guardan en variables de la colección, nunca en el archivo.

## Pruebas unitarias

| Proyecto | Comando | Resultado |
|---|---|---|
| PharmaBackend | `./mvnw test` | 24 pruebas, 0 fallos |
| pharma-frontend | `ng test --watch=false` | 5 pruebas, 0 fallos |
