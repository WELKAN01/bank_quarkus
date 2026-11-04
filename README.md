# Banking API

API reactiva para la gestión de cuentas bancarias, construida con Java 17 y Quarkus. El proyecto sigue una arquitectura hexagonal: el dominio contiene las reglas de negocio, la aplicación orquesta los casos de uso y la infraestructura adapta HTTP, PostgreSQL y servicios externos.

## Tecnologías

- Java 17 y Quarkus 3.40.
- REST con JSON y Mutiny (`Uni`).
- PostgreSQL con Hibernate Reactive Panache y Flyway.
- OAuth 2.0 / OpenID Connect mediante Quarkus OIDC y Amazon Cognito.
- OpenAPI, Health Checks y pruebas con JUnit 5 / REST Assured.

## Estructura

```text
src/main/java/org/transaction/account/
├── domain/          # Modelo, validaciones, puertos y excepciones
├── application/     # Implementación de los casos de uso
└── infrastructure/  # Recursos REST, persistencia y cliente de tipo de cambio
```

## Requisitos

- JDK 17.
- PostgreSQL 12.16 o compatible.
- Maven 3.9+ (o el wrapper incluido en el repositorio).

## Configuración

`src/main/resources/application.properties` es configuración local y está excluido de Git para evitar publicar credenciales. Debe existir en cada entorno de desarrollo. Usa `application-remote.properties` como referencia para mantener las mismas claves de configuración.

Las siguientes variables permiten sustituir valores sin editar archivos:

| Variable | Uso | Valor local predeterminado |
| --- | --- | --- |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `123456` |
| `DB_REACTIVE_URL` | URL reactiva (`postgresql://host:puerto/base`) | `postgresql://localhost:5432/banking_db` |
| `DB_JDBC_URL` | URL JDBC para Flyway | `jdbc:postgresql://localhost:5432/banking_db` |
| `OIDC_AUTH_SERVER_URL` | URL del emisor OIDC remoto | — |
| `OIDC_CLIENT_ID` | Identificador del cliente OIDC remoto | — |

Para ejecutar con el perfil remoto, configura las variables requeridas y usa:

```powershell
.\mvnw.cmd quarkus:dev -Dquarkus.profile=remote
```

> El perfil remoto requiere TLS para el cliente reactivo de PostgreSQL y valida el esquema ORM. El perfil de desarrollo actualiza el esquema automáticamente; el de producción lo valida.

## Ejecutar la aplicación

```powershell
.\mvnw.cmd quarkus:dev
```

La aplicación queda disponible normalmente en `http://localhost:8080`. La documentación OpenAPI se sirve en `http://localhost:8080/q/swagger-ui` durante desarrollo y el estado de salud en `http://localhost:8080/q/health`.

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

Para empaquetar la aplicación:

```powershell
.\mvnw.cmd package
```

## Endpoints

Todos los endpoints consumen y producen `application/json`.

| Método | Ruta | Descripción | Autorización |
| --- | --- | --- | --- |
| `POST` | `/accounts/create-account` | Crea una cuenta. | No declarada |
| `POST` | `/accounts/create-accounts` | Crea varias cuentas. | No declarada |
| `POST` | `/accounts/deposito` | Deposita fondos. | No declarada |
| `POST` | `/accounts/extraccion` | Retira fondos. | No declarada |
| `PATCH` | `/accounts/Update-account` | Actualiza nombre o moneda de una cuenta. | No declarada |
| `GET` | `/accounts/Info_cuentas` | Lista cuentas paginadas. | Rol `ADMIN` |
| `GET` | `/accounts/get_profile_sesion` | Obtiene reclamos del token de sesión. | Rol `ADMIN` |
| `GET` | `/hello` | Endpoint de comprobación simple. | No declarada |

Ejemplo de creación:

```json
{
  "balance": 1000.00,
  "moneda": "PEN",
  "nombre_dueino": "Ada Lovelace",
  "numero_cuenta": "00123456789",
  "numero_interbbancario": "00200123456789012345"
}
```

Ejemplo de depósito o retiro:

```json
{
  "numero_cuenta": "00123456789",
  "cuenta_interbancaria": "00200123456789012345",
  "amount": 250.00,
  "moneda": "PEN"
}
```

Ejemplo de actualización:

```json
{
  "numero_cuenta": "00123456789",
  "cuenta_interbancaria": "00200123456789012345",
  "cambios": {
    "nombre_dueino": "Ada Byron",
    "moneda": "USD"
  }
}
```

Las operaciones administrativas requieren un token Bearer válido cuyo claim `cognito:groups` incluya el grupo `ADMIN`.

## Reglas de dominio

- El saldo inicial no puede ser negativo.
- Los depósitos y retiros deben ser mayores que cero.
- Un retiro no puede exceder el saldo disponible.
- El nombre del titular y la moneda no pueden estar vacíos; la moneda se normaliza a mayúsculas.

## Servicio externo

La aplicación consulta tipos de cambio mediante la API de Frankfurter. La URL base se configura con `quarkus.rest-client.exchange-api.url`.
