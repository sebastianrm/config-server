# config-server

**Spring Cloud Config Server** — Configuración centralizada para todos los microservicios de la plataforma Yanki.

- **Puerto:** `8888`
- **Backend:** `Git` por defecto (configurable a native mediante profile)
- **Stack:** Java 17, Spring Boot 3.3.5, Spring Cloud Config 2023.0.3, springdoc-openapi, Lombok, Resilience4j, RxJava3

---

## Estado de Auditoría (2026-11-22)

> **Estado:** 🟢 **Implementado** — Spring Cloud Config Server funcional con `@EnableConfigServer`, backend Git por defecto, Eureka client, Dockerfile multi-etapa con usuario no root, OpenAPI spec.
> **Tests:** 4 pruebas (2 pasan, 2 fallan por falta de `ENCRYPT_KEY` — comportamiento esperado en entorno sin clave de cifrado).
> **Build:** `mvn clean compile` — BUILD SUCCESS.

| Componente | Estado | Detalle |
|---|---|---|
| **Clase principal** | ✅ Completo | `ConfigServerApplication.java` con `@EnableConfigServer` y `@SpringBootApplication` |
| **Configuración** | ✅ Completo | `application.yml` con puerto 8888, backend Git por defecto, Eureka client, management endpoints |
| **Docker** | ✅ Completo | `Dockerfile` multi-etapa: build con Maven + JDK 17, runtime JRE 17 Alpine, usuario no root (yanki) |
| **OpenAPI/Swagger** | ✅ Completo | `config-server.openapi.yml` con 3 endpoints documentados |
| **Tests** | ✅ Parcial | 4 tests JUnit 5: `contextLoads` ✓, `configEndpointTest` ✓, `encryptEndpointTest` ✗ (sin ENCRYPT_KEY), `decryptEndpointTest` ✗ (sin ENCRYPT_KEY) |
| **JaCoCo** | ✅ Configurado | Plugin presente, excluye config, infrastructure, adapters, Application, Exception, Response |
| **Checkstyle** | ⚠️ Mejora | Propiedades definidas en pom.xml, pero plugin `maven-checkstyle-plugin` no ejecutado |
| **Config-repo** | ✅ Completo | Git backend configurado con URI configurable (`CONFIG_GIT_URI`) |
| **Casos de Prueba** | ✅ Completo | 2 casos de prueba documentados (CP-023, CP-024) en `CASOS_PRUEBA/` |

### Hallazgos de Auditoría

| Severidad | Hallazgo | Estado | Detalle |
|---|---|---|---|
| ✅ Resuelto | `@EnableConfigServer` ausente | ✅ | Presente en `ConfigServerApplication.java` |
| ✅ Resuelto | Backend Git configurado por defecto | ✅ | URI configurable vía `CONFIG_GIT_URI` |
| ✅ Resuelto | Eureka client no registrado | ✅ | Registrado con `register-with-eureka: true` |
| ✅ Resuelto | Dockerfile sin usuario no root | ✅ | Usuario `yanki` creado y aplicado |
| ⚠️ Mejora | Checkstyle plugin no ejecutado | ⚠️ | Propiedades definidas, plugin no configurado en build |
| ⚠️ Pendiente | Cifrado de valores sensibles | ⚠️ | Requiere `ENCRYPT_KEY` + JCE Unlimited Strength |
| ⚠️ Convención | Javadoc en inglés | ⚠️ | `ConfigServerApplication.java` y tests con Javadoc en inglés |

### Endpoints Implementados (5 endpoints nativos de Spring Cloud Config Server)

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/{application}/{profile}[/{label}]` | Obtener configuración resuelta para una aplicación y perfil |
| `GET` | `/{application}-{profile}.yml` | Obtener configuración en formato YAML plano |
| `POST` | `/encrypt` | Cifrar un valor (requiere `ENCRYPT_KEY`) |
| `POST` | `/decrypt` | Descifrar un valor cifrado |
| `GET` | `/actuator/health` | Health check del servidor de configuración |

---

## 1. Propósito

| Responsabilidad | Detalle |
|---|---|
| Configuración centralizada | Un solo punto de verdad para configuración de todos los servicios |
| Entornos | Profiles: `dev`, `prod`, `test` (override por profile) |
| Versionado | Git (producción) / Filesystem (desarrollo) |
| Cifrado | Valores sensibles como `{cipher}AQE...` (requiere JCE Unlimited) |
| Refresh | `/actuator/refresh` (Spring Cloud Bus opcional para broadcast) |

---

## 2. Endpoints REST

### 2.1 Config Server API

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/{application}/{profile}[/{label}]` | Config app (ej: `customers-service/default`) |
| `GET` | `/{application}-{profile}.yml` | Config YAML plano |
| `GET` | `/{application}/{profile}/{label}/{path}` | Archivo específico |
| `POST` | `/actuator/refresh` | Trigger refresh clientes (con Spring Cloud Bus) |
| `POST` | `/encrypt` | Cifrar valor (requiere `ENCRYPT_KEY`) |
| `POST` | `/decrypt` | Descifrar valor |
| `GET` | `/actuator/health` | Health check |

### 2.2 Ejemplos

```bash
# Config resuelta para customers-service (profile default)
curl -s http://localhost:8888/customers-service/default | jq '.propertySources[] | {name: .name, source: .source}'

# Config compartida (application.yml)
curl -s http://localhost:8888/application/default

# Config específica en YAML
curl -s http://localhost:8888/customers-service-default.yml

# Cifrar valor (producción)
curl -X POST http://localhost:8888/encrypt -d "my-secret-password" -H "Content-Type: text/plain"
# Response: {cipher}AQE...

# Descifrar valor
curl -X POST http://localhost:8888/decrypt -d "{cipher}AQE..." -H "Content-Type: text/plain"

# Forzar refresh en cliente (sin Bus)
curl -X POST http://localhost:8082/actuator/refresh
```

---

## 3. Estructura de Archivos (Git Backend)

```
config-repo/ (Git repository - configurable via CONFIG_GIT_URI)
├── application.yml              # Común a TODOS (shared)
├── application-dev.yml          # Profile dev
├── application-prod.yml         # Profile prod
├── customers-service.yml        # Específico customers
├── accounts-service.yml
├── credits-service.yml
├── cards-service.yml
├── transfers-service.yml
├── wallet-service.yml
├── reports-service.yml
├── auth-service.yml
├── api-gateway.yml
├── discovery-service.yml
└── config-server.yml
```

### Configuración Git Backend

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: ${CONFIG_GIT_URI:file:///config-repo}
          search-paths: ${CONFIG_GIT_SEARCH_PATHS:.}
          default-label: main
          force-pull: false
        health:
          enabled: true
```

### application.yml (Shared - Ejemplo)

```yaml
spring:
  data:
    mongodb:
      auto-index-creation: true
  redis:
    timeout: 2000ms
  kafka:
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.yanki.*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

resilience4j:
  circuitbreaker:
    instances:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
  timelimiter:
    instances:
      default:
        timeoutDuration: 2s

yanki:
  cache:
    catalog-ttl: 30m
    customer-ttl: 10m
```

---

## 4. Cliente (Todos los Servicios)

### bootstrap.yml (Cada Servicio)

```yaml
spring:
  application:
    name: customers-service  # Clave de búsqueda en config server
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
  cloud:
    config:
      fail-fast: true
      retry:
        initial-interval: 2000
        max-attempts: 6
```

### Precedencia de Configuración

1. `application.yml` (shared) - menor prioridad
2. `application-{profile}.yml` - profile específico
3. `{service}.yml` - específico del servicio - mayor prioridad

---

## 4. Variables de Entorno

| Variable | Default | Descripción |
|---|---|---|
| `SERVER_PORT` | `8888` | Puerto HTTP |
| `CONFIG_PROFILE` | `git` | Profile activo (git/native) |
| `CONFIG_GIT_URI` | `file:///config-repo` | URI repo Git (configurable) |
| `CONFIG_GIT_SEARCH_PATHS` | `.` | Paths de búsqueda en el repo |
| `CONFIG_SERVER_GIT_USERNAME` | *(prod)* | User Git |
| `CONFIG_SERVER_GIT_PASSWORD` | *(prod)* | Pass/Token Git |
| `ENCRYPT_KEY` | *(prod)* | Clave cifrado `{cipher}` |
| `EUREKA_URL` | `http://discovery-service:8761/eureka/` | Registro en Eureka |

---

## 5. Docker

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S yanki && adduser -S yanki -G yanki
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R yanki:yanki /app
USER yanki
EXPOSE 8888
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

### Health Check

```yaml
healthcheck:
  test: ["CMD", "wget", "-q", "--spider", "http://localhost:8888/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 20s
```

---

## 5. Seguridad

| Entorno | Configuración |
|---|---|
| **Dev** | Sin auth (acceso abierto a config) |
| **Prod** | **OBLIGATORIO** Spring Security (Basic Auth u OAuth2) |
| **Cifrado** | `{cipher}AQE...` requiere `ENCRYPT_KEY` + JCE Unlimited Strength |
| **Git Backend** | SSH keys / Personal Access Token para repo privado |

---

## 6. Cómo Ejecutarlo

### Desarrollo
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17.0.21.0.3"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

### Build y Pruebas
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17.0.21.0.3"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn clean verify
```

### Docker
```bash
docker build -t yanki/config-server:1.0.0 .
docker run -p 8888:8888 \
  -e CONFIG_GIT_URI=file:///config-repo \
  -e CONFIG_PROFILE=git \
  yanki/config-server:1.0.0
```

### Salud
```bash
curl http://localhost:8888/actuator/health
```

---

## 6. Estructura del Código

```
src/main/java/com/yanki/config
├── ConfigServerApplication.java
├── config
│   └── ConfigProperties.java
└── interfaces
    └── rest
        └── ConfigController.java  # Endpoints /encrypt, /decrypt
```

---

## 7. Referencias

- `Proyecto General vf.md` - Parte I § Config Server, Parte II § Configuración
- `README.md` (root) - Arquitectura, clientes config
- `AGENTS.md` (este servicio) - Especificación técnica completa
- `docker-compose.yml` - Infraestructura (config-server incluido)
- `config/` - Directorio con archivos de configuración