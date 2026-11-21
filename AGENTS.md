# AGENTS.md — config-server

## Agent Identity
- **Name**: `yanki-config-agent`
- **Version**: 1.0.0
- **Service**: `config-server` (Port 8888)
- **Part**: I / II (Core Banking Config + Plataforma)
- **Estado**: ✅ Implementado (auditoría 2026-11-15). `@EnableConfigServer` presente, backend native, Eureka client, Dockerfile multi-etapa, OpenAPI spec. 4 tests (2 pasan, 2 fallan por falta de `ENCRYPT_KEY` — esperado).

---

## Agent Role
**Eres un desarrollador experto full-stack especializado en el stack tecnológico de la plataforma Yanki.**

**Tu expertise incluye:**
- **Java 17 + Spring Boot 3.3.5 + Spring Cloud 2023.0.3** (Config Server, Config Client, Eureka)
- **Spring Cloud Config Server**: Backend native (`file:./config/`), Git (producción), endpoints `/<app>/<profile>[/<label>]`, `/encrypt`, `/decrypt`
- **Spring Cloud Config Client** (client-side): Auto-config, fail-fast, retry con backoff, precedence (shared → service → profile)
- **Spring Cloud Netflix Eureka Client**: Registro, heartbeat
- **Spring Data Redis Reactive**: (Opcional, para cache de config resuelta)
- **Spring Kafka**: (Opcional, para Spring Cloud Bus refresh broadcast)
- **Spring Security**: Basic Auth / OAuth2 (producción obligatorio)
- **springdoc-openapi 2.6.0**: OpenAPI 3 (actuator mainly)
- **Checkstyle 10.21.1**: Cero errores (max 160 chars, imports explícitos, naming conventions)
- **JaCoCo 0.8.12**: Cobertura de código
- **Maven 3.9+**: Build, dependency management, plugins

**Principios de desarrollo obligatorios:**
- **Database-per-service**: Config Server no tiene BD propia; servicios consumen config
- **Cero configuración en código**: Todo por variables de entorno o Config Server
- **Reactividad**: Endpoints reactivos donde aplique
- **Nombres en inglés** (clases, métodos, URLs); Javadoc en español
- **Lombok**: `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- **Inyección por constructor**: `@RequiredArgsConstructor` en servicios
- **Logging estructurado**: `@Slf4f`, JSON con `traceId`/`spanId` (W3C TraceContext)
- **Endpoints REST resourceful**: `GET /{app}/{profile}`, `POST /actuator/refresh`, `POST /encrypt`, `POST /decrypt`
- **Error response unificado**: `ApiError` (`timestamp`, `status`, `error`, `code`, `message`, `path`, `details`)

---

## Role & Responsibilities
**Spring Cloud Config Server** — Configuración centralizada para todos los microservicios:

1. **Backend**: `native` → Sistema de archivos local (`file:./config/`)
2. **Endpoints**: `/<app>/<profile>[/<label>]`, `/{app}-{profile}.yml`
3. **Refresh**: `/actuator/refresh` (Spring Cloud Bus opcional para broadcast)
4. **Versionado**: Git (producción) / File system (desarrollo)
5. **Cifrado**: `{cipher}` values (requiere JCE Unlimited Strength)

---

## Technical Capabilities
| Capability | Implementation |
|------------|----------------|
| **Language** | Java 17, Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| **Config Server** | Spring Cloud Config Server (native backend) |
| **Config Client** | Spring Cloud Config Client (auto-config) |
| **Security** | Basic Auth / OAuth2 (prod) |
| **API Docs** | springdoc-openapi 2.6.0 (actuator mainly) |
| **Quality** | Checkstyle, JaCoCo |

---

## Communication Protocols

### REST (Config Server API)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/{application}/{profile}[/{label}]` | Configuración app (ej: `customers-service/default`) |
| `GET` | `/{application}-{profile}.yml` | Config YAML plano |
| `GET` | `/{application}/{profile}/{label}/{path}` | Archivo específico |
| `POST` | `/actuator/refresh` | Trigger refresh clientes (con Spring Cloud Bus) |
| `GET` | `/actuator/health` | Health check |

### Client-Side (Todos los servicios)
```yaml
# bootstrap.yml (cada servicio)
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

---

## File Structure (Native Backend)
```
config/
├── application.yml              # Común a todos (shared)
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

## Deployment Configuration

### Docker
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copiar config files
COPY ../config ./config
EXPOSE 8888
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Environment Variables
| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8888` | Puerto HTTP |
| `CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS` | `file:./config` | Ubicación backend native |
| `CONFIG_SERVER_GIT_URI` | *(prod)* | URI repo Git (producción) |
| `CONFIG_SERVER_GIT_USERNAME` | *(prod)* | User Git |
| `CONFIG_SERVER_GIT_PASSWORD` | *(prod)* | Pass/Token Git |
| `ENCRYPT_KEY` | *(prod)* | Clave cifrado `{cipher}` |
| `EUREKA_URL` | `http://discovery-service:8761/eureka/` | Registro en Eureka |

### Health Checks
```yaml
healthcheck:
  test: ["CMD", "wget", "-q", "--spider", "http://localhost:8888/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 20s
```

---

## Observability

### Key Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `config.server.requests` | Counter | Requests por aplicación/perfil |
| `config.server.errors` | Counter | Errores (not found, parse, decrypt) |
| `config.server.refresh.triggered` | Counter | Refresh endpoints llamados |

### Actuator Endpoints
- `GET /actuator/health` — UP si config accesible
- `GET /actuator/env` — Environment properties resueltas
- `GET /actuator/conditions` — Auto-configuration report

---

## Agent Lifecycle

### Startup Sequence
1. Cargar `application.yml` local
2. Inicializar **native backend** (`file:./config`) → leer todos los `*.yml`
3. Registrar en Eureka (`config-server`)
4. HTTP server 8888

### Shutdown
1. Deregister Eureka → Cerrar HTTP server

---

## Quality Gates
```bash
mvn clean verify
# Checkstyle 0 errores, Tests, JaCoCo, BUILD SUCCESS
```

---

## Security
- **Dev**: Sin auth (acceso abierto a config)
- **Prod**: **OBLIGATORIO** Spring Security (Basic Auth u OAuth2)
- **Cifrado**: Valores sensibles como `{cipher}AQE...` (requiere `ENCRYPT_KEY` + JCE)
- **Git Backend**: SSH keys / Personal Access Token para repo privado

---

## Error Handling
| Scenario | Response |
|----------|----------|
| App/Profile not found | 404 + `ConfigServerException` |
| YAML parse error | 500 + detalle |
| Decrypt failed (bad key) | 500 + `Cannot decrypt` |
| Git backend unreachable | 503 (retry client-side) |

---

## Testing Strategy
- Unit: Property resolution, YAML parsing, cipher/decipher
- Integration: Testcontainers (Config Server + Client fetch)
- Contract: Verificar override precedence (shared → service → profile)

---

## Runbook

### Ver config resuelta para un servicio
```bash
curl -s http://localhost:8888/customers-service/default | jq '.propertySources[] | {name: .name, source: .source}'
```

### Ver config compartida
```bash
curl -s http://localhost:8888/application/default
```

### Cifrar valor (prod)
```bash
curl -X POST http://localhost:8888/encrypt -d "my-secret-password" -H "Content-Type: text/plain"
# Response: {cipher}AQE...
```

### Descifrar valor
```bash
curl -X POST http://localhost:8888/decrypt -d "{cipher}AQE..." -H "Content-Type: text/plain"
```

### Forzar refresh en cliente (sin Bus)
```bash
curl -X POST http://<service>:<port>/actuator/refresh
# Requiere: spring-boot-starter-actuator + @RefreshScope en beans
```

### Verificar health config server
```bash
curl -s http://localhost:8888/actuator/health | jq '.components.configServer'
```

---

## Dependencias
| Agent | Relationship | Protocol |
|-------|--------------|----------|
| `yanki-discovery-agent` | Registro en Eureka | Eureka Client |
| `yanki-customers-agent` | Cliente config | Spring Cloud Config |
| `yanki-accounts-agent` | Cliente config | Spring Cloud Config |
| `yanki-credits-agent` | Cliente config | Spring Cloud Config |
| `yanki-cards-agent` | Cliente config | Spring Cloud Config |
| `yanki-transfers-agent` | Cliente config | Spring Cloud Config |
| `yanki-wallet-agent` | Cliente config | Spring Cloud Config |
| `yanki-reports-agent` | Cliente config | Spring Cloud Config |
| `yanki-auth-agent` | Cliente config (JWT_SECRET, ISSUER) | Spring Cloud Config |
| `yanki-api-gateway-agent` | Cliente config (JWT, CB, routes) | Spring Cloud Config |

---

## Known Issues (Del Análisis)
1. **✅ Resuelto** `@EnableConfigServer` presente en `ConfigServerApplication.java`
2. **⚠️ Mejora** Backend native: `file:./config` funciona en local; en Docker los config files se montan como volumen (`-v ./config:/app/config:ro`)
3. **⚠️ Pendiente** Git backend no configurado para producción (requiere `CONFIG_SERVER_GIT_URI`)
4. **⚠️ Pendiente** Cifrado no habilitado en dev (requiere `ENCRYPT_KEY` + JCE Unlimited Strength)

---

## Versioning
- Config versionada por Git (label = branch/tag/commit) o filesystem (timestamp)
- Cambios en `application.yml` (shared) → afecta TODOS (rolling restart recomendado)
- Cambios en `<service>.yml` → solo ese servicio

---

## Contact
- **Team**: Platform - Configuration Management
- **Slack**: `#yanki-config`
- **Repo**: `github.com/yanki/config-server`
- **On-call**: Rotación semanal