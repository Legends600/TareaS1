#!/usr/bin/env bash
# Pruebas del paso 14 (Guia S10): accesos permitidos y denegados.
# Uso: bash pruebas-seguridad/pruebas_paso14.sh  (con la API corriendo en :8080)
# Requiere curl y node. Las pruebas 20-22 crean el usuario de Maria: en una
# segunda ejecucion la 20 respondera 409 porque el usuario ya existe.

API=${API:-http://localhost:8080}
PRODUCTO_ID=${PRODUCTO_ID:-59}
CLIENTE_ID=${CLIENTE_ID:-21}

json() { node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{const o=JSON.parse(d);console.log($1)})"; }

prueba() { # n, descripcion, esperado, curl args...
  local n=$1 desc=$2 esperado=$3; shift 3
  local salida codigo cuerpo
  salida=$(curl -s -w $'\n%{http_code}' "$@")
  codigo=${salida##*$'\n'}
  cuerpo=${salida%$'\n'*}
  local ok="OK"; [[ "$codigo" == "$esperado" ]] || ok="FALLA"
  echo "### Prueba $n [$ok] - $desc"
  echo "HTTP $codigo (esperado $esperado)"
  echo "$cuerpo" | head -c 700; echo; echo
}

login() { curl -s -X POST "$API/api/v1/auth/login" -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" | json "o.token"; }

prueba 1 "Sin token GET /api/health" 200 "$API/api/health"
prueba 2 "Sin token GET /api/v1/productos" 401 "$API/api/v1/productos"
prueba 3 "Login admin con contrasena incorrecta" 401 -X POST "$API/api/v1/auth/login" \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"incorrecta"}'
prueba 4 "Login inactivo / Inactivo2026*" 401 -X POST "$API/api/v1/auth/login" \
  -H "Content-Type: application/json" -d '{"username":"inactivo","password":"Inactivo2026*"}'
prueba 5 "Login con username vacio" 400 -X POST "$API/api/v1/auth/login" \
  -H "Content-Type: application/json" -d '{"username":"","password":"x"}'
prueba 6 "Login vendedor / Vendedor2026*" 200 -X POST "$API/api/v1/auth/login" \
  -H "Content-Type: application/json" -d '{"username":"vendedor","password":"Vendedor2026*"}'

VEND=$(login vendedor 'Vendedor2026*')
ALM=$(login almacen 'Almacen2026*')
ADM=$(login admin 'Admin2026*')
SUP=$(login supervisor 'Supervisor2026*')

echo "### Prueba 7 - Payload del token del vendedor (Base64 decodificado)"
echo "$VEND" | cut -d. -f2 | node -e "let d='';process.stdin.on('data',c=>d+=c).on('end',()=>{const p=JSON.parse(Buffer.from(d.trim(),'base64url'));console.log(JSON.stringify(p));console.log('exp - iat =',p.exp-p.iat)})"
echo

prueba 8 "vendedor GET /api/v1/auth/me" 200 -H "Authorization: Bearer $VEND" "$API/api/v1/auth/me"
prueba 9 "vendedor GET /api/v1/productos" 200 -H "Authorization: Bearer $VEND" "$API/api/v1/productos"
prueba 10 "vendedor DELETE /api/v1/productos/1" 403 -X DELETE -H "Authorization: Bearer $VEND" "$API/api/v1/productos/1"
VENTA="{\"clienteId\":$CLIENTE_ID,\"detalles\":[{\"productoId\":$PRODUCTO_ID,\"cantidad\":1}]}"
prueba 11 "vendedor POST /api/v1/ventas" 201 -X POST -H "Authorization: Bearer $VEND" \
  -H "Content-Type: application/json" -d "$VENTA" "$API/api/v1/ventas"
prueba 12 "vendedor GET /api/v1/reportes/ventas-por-categoria" 403 -H "Authorization: Bearer $VEND" \
  "$API/api/v1/reportes/ventas-por-categoria"
prueba 13 "almacen POST /api/v1/ventas" 403 -X POST -H "Authorization: Bearer $ALM" \
  -H "Content-Type: application/json" -d "$VENTA" "$API/api/v1/ventas"

PRODUCTO=$(curl -s -H "Authorization: Bearer $ADM" "$API/api/v1/productos/$PRODUCTO_ID" \
  | json "JSON.stringify({nombre:o.nombre,precio:o.precio,stock:o.stock,estado:o.estado,categoriaId:o.categoriaId})")
# El cuerpo va en un archivo: en Windows, pasar tildes por argumento rompe el UTF-8
CUERPO_PRODUCTO=$(mktemp)
printf "%s" "$PRODUCTO" > "$CUERPO_PRODUCTO"
prueba 14 "almacen PUT /api/v1/productos/$PRODUCTO_ID" 200 -X PUT -H "Authorization: Bearer $ALM" \
  -H "Content-Type: application/json" --data-binary "@$CUERPO_PRODUCTO" "$API/api/v1/productos/$PRODUCTO_ID"
prueba 15 "admin GET /api/v1/reportes/ventas-por-categoria" 200 -H "Authorization: Bearer $ADM" \
  "$API/api/v1/reportes/ventas-por-categoria"
prueba 16 "Login supervisor / Supervisor2026*" 200 -X POST "$API/api/v1/auth/login" \
  -H "Content-Type: application/json" -d '{"username":"supervisor","password":"Supervisor2026*"}'
prueba 17a "supervisor POST /api/v1/ventas" 201 -X POST -H "Authorization: Bearer $SUP" \
  -H "Content-Type: application/json" -d "$VENTA" "$API/api/v1/ventas"
prueba 17b "supervisor PUT /api/v1/productos/$PRODUCTO_ID" 200 -X PUT -H "Authorization: Bearer $SUP" \
  -H "Content-Type: application/json" --data-binary "@$CUERPO_PRODUCTO" "$API/api/v1/productos/$PRODUCTO_ID"
prueba 18 "supervisor GET /api/v1/reportes/ventas-por-categoria" 403 -H "Authorization: Bearer $SUP" \
  "$API/api/v1/reportes/ventas-por-categoria"
prueba 19a "admin GET /api/v1/empleados" 200 -H "Authorization: Bearer $ADM" "$API/api/v1/empleados"
prueba 19b "admin GET /api/v1/roles" 200 -H "Authorization: Bearer $ADM" "$API/api/v1/roles"

MARIA=$(curl -s -H "Authorization: Bearer $ADM" "$API/api/v1/empleados" | json "o.find(e=>e.dni==='45667788').id")
VENDEDOR_ROL=$(curl -s -H "Authorization: Bearer $ADM" "$API/api/v1/roles" | json "o.find(r=>r.nombre==='VENDEDOR').id")
# La 22 va antes de la 20: con Maria aun sin usuario, el servicio llega a
# validar los roles y responde 404. Despues del alta, la regla 1 a 1 (409) gana.
prueba 22 "admin alta con rolIds [2, 99]" 404 -X POST -H "Authorization: Bearer $ADM"   -H "Content-Type: application/json"   -d "{\"username\":\"otro.user\",\"password\":\"Clave2026\",\"empleadoId\":$MARIA,\"rolIds\":[2,99]}"   "$API/api/v1/usuarios"
prueba 20 "admin POST /api/v1/usuarios para Maria" 201 -X POST -H "Authorization: Bearer $ADM" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"mhuaman\",\"password\":\"Cajera2026\",\"empleadoId\":$MARIA,\"rolIds\":[$VENDEDOR_ROL]}" \
  "$API/api/v1/usuarios"
prueba 21 "admin repite alta con el mismo empleadoId" 409 -X POST -H "Authorization: Bearer $ADM" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"mhuaman2\",\"password\":\"Cajera2026\",\"empleadoId\":$MARIA,\"rolIds\":[$VENDEDOR_ROL]}" \
  "$API/api/v1/usuarios"
ALTERADO="${VEND:0:${#VEND}-2}$([[ $VEND == *AA ]] && echo BB || echo AA)"
prueba 23 "vendedor con token alterado" 401 -H "Authorization: Bearer $ALTERADO" "$API/api/v1/productos"
if [[ -n "$TOKEN_VENCIDO" ]]; then
  prueba 24 "vendedor con token vencido" 401 -H "Authorization: Bearer $TOKEN_VENCIDO" "$API/api/v1/productos"
else
  echo "### Prueba 24 - Exporta TOKEN_VENCIDO con un token emitido con expiracion-minutos: 1 hace mas de 1 minuto"
fi
