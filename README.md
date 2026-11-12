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

## CI/CD con GitHub Actions y AWS

El repositorio incluye una canalización lista para desplegar el contenedor en Amazon ECS Fargate:

- **CI** (`.github/workflows/ci.yml`): se ejecuta en cada pull request y cambio a `main`; ejecuta `mvn verify`, conserva los reportes de pruebas y valida que la imagen Docker pueda construirse.
- **CD** (`.github/workflows/deploy-aws.yml`): en `main` (o manualmente) vuelve a verificar el código, publica una imagen inmutable etiquetada con el SHA en ECR y actualiza el servicio ECS. El environment `production` permite exigir aprobación desde GitHub antes del despliegue.

Antes del primer despliegue, crea el repositorio ECR, un clúster/servicio ECS Fargate, el grupo de logs `/ecs/banking-api` y completa los marcadores `REPLACE_WITH_...` de `infrastructure/ecs/task-definition.json`. El balanceador debe comprobar `GET /q/health/live` en el puerto 8080.

Configura estas **variables de repositorio** en GitHub: `AWS_REGION`, `ECR_REPOSITORY`, `ECS_CLUSTER` y `ECS_SERVICE`. Configura el secreto `AWS_DEPLOY_ROLE_ARN` con el ARN del rol que GitHub asumirá. No guardes contraseñas ni URLs de base de datos en GitHub: el task definition las obtiene desde AWS Secrets Manager como `DB_USER`, `DB_PASSWORD`, `DB_REACTIVE_URL`, `DB_JDBC_URL`, `OIDC_AUTH_SERVER_URL` y `OIDC_CLIENT_ID`.

El proveedor OIDC de GitHub debe ser `https://token.actions.githubusercontent.com` con audiencia `sts.amazonaws.com`. Restringe la política de confianza del rol al repositorio y a `refs/heads/main` (y, si se usará, al environment `production`). Ese rol necesita únicamente permisos para publicar en el repositorio ECR indicado y registrar/actualizar la definición y servicio ECS. El **execution role** de ECS, por su parte, necesita leer los secretos y escribir logs; el **task role** debe quedar con permisos de aplicación mínimos.
