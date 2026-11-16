# Supermercado MCP

Servidor MCP HTTP para consultar el catálogo, registrar clientes y crear/consultar
pedidos a domicilio. Usa Spring Boot, Spring AI MCP, Spring Data JPA y PostgreSQL.

## Requisitos y ejecución

- Java 21
- PostgreSQL con una base de datos llamada `labmcp`

Para crear la base y sus tablas manualmente, ejecuta `CREATE DATABASE labmcp;`
conectado a PostgreSQL y luego ejecuta `src/main/resources/schema.sql` conectado
a la base `labmcp`. Con `psql`, por ejemplo:

```powershell
psql -U postgres -d postgres -c "CREATE DATABASE labmcp;"
psql -U postgres -d labmcp -f src/main/resources/schema.sql
```

La conexión se configura con `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Si no se
definen, se usa `jdbc:postgresql://localhost:5432/labmcp`, usuario `postgres` y
contraseña vacía. Ejemplo en PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/labmcp"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "tu-clave-local"
.\mvnw.cmd spring-boot:run
```

El servidor escucha en el puerto `8088` y ofrece MCP por HTTP en
`http://localhost:8088/mcp`. En esta configuración Hibernate actualiza el
esquema automáticamente (`ddl-auto=update`), apropiado para desarrollo; para
producción se recomienda administrar el esquema con migraciones.

Para iniciar la negociación MCP con JSON-RPC desde PowerShell, ejecuta la
aplicación y luego:

```powershell
$body = @{
    jsonrpc = "2.0"
    id = 1
    method = "initialize"
    params = @{
        protocolVersion = "2025-03-26"
        capabilities = @{}
        clientInfo = @{
            name = "powershell"
            version = "1.0"
        }
    }
} | ConvertTo-Json -Depth 8

$response = Invoke-RestMethod -Method Post `
    -Uri "http://localhost:8088/mcp" `
    -ContentType "application/json" `
    -Headers @{ Accept = "application/json, text/event-stream" } `
    -Body $body

$response | ConvertTo-Json -Depth 8
```

La respuesta de `initialize` incluye la versión de protocolo negociada y
capacidades del servidor. Esta solicitud inicia una sesión MCP; los clientes
MCP completos también envían `notifications/initialized` antes de invocar
herramientas.

## Herramientas MCP

- `registrarCliente` y `buscarClientePorCedula`
- `registrarProducto`, `listProductos` y `buscarProductos`
- `crearPedido`, `listarPedidosCliente` y `consultarPedido`

`crearPedido` recibe el ID del cliente, listas paralelas de IDs de productos y
cantidades, y la dirección de entrega. Las cantidades deben ser positivas; los
IDs de producto repetidos se agrupan. El precio unitario queda guardado en cada
línea del pedido para preservar el total histórico si cambia el catálogo.

El servidor no implementa autenticación ni control de inventario. Ejecútelo solo
en un entorno confiable y agregue esos controles antes de exponerlo públicamente.
