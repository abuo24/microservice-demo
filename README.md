# Microservices Demo

Production-grade microservices architecture built with **Kotlin + Spring Boot 3.5 + Kubernetes**. Demonstrates Event Sourcing, CQRS, Outbox Pattern, Consumer-Driven Contract Testing, and full observability stack.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Patterns Implemented](#patterns-implemented)
- [Services](#services)
- [Quick Start](#quick-start)
- [API Usage](#api-usage)
- [Testing Strategy](#testing-strategy)
- [Observability](#observability)
- [Project Structure](#project-structure)
- [Configuration Reference](#configuration-reference)
- [Troubleshooting](#troubleshooting)

---

## Architecture

```
                        ┌────────────────────────────────────────────┐
                        │           Kubernetes Cluster               │
                        │                                            │
Internet ──► Ingress ──►│  api-gateway :8080                         │
                        │      │                                     │
                        │      ├──► order-service :8081              │
                        │      │         │  ┌──────────────────┐     │
                        │      │         ├──┤ orders DB        │     │
                        │      │         │  │ order_events     │     │
                        │      │         │  │ order_projections│     │
                        │      │         │  └──────────────────┘     │
                        │      │         │                           │
                        │      │         │  ┌──────────────────┐     │
                        │      │         └──┤ Kafka Topics     │     │
                        │      │            │ - order-created  │     │
                        │      │            │ - order-status-  │     │
                        │      │            │   changed        │     │
                        │      │            └──────────────────┘     │
                        │      │                  │                  │
                        │      │                  ▼                  │
                        │      └──► inventory-service :8082          │
                        │                                            │
                        │  Infra: Postgres • Redis • Kafka           │
                        │  Auth:  Keycloak (JWT/OAuth2)              │
                        │  Observability: Zipkin • Prometheus •      │
                        │                 Grafana • Loki             │
                        └────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Kotlin 2.2 |
| **Framework** | Spring Boot 3.5 (WebMVC for services, WebFlux for gateway) |
| **Build** | Gradle (Groovy DSL), Skaffold for K8s |
| **Security** | Spring Security + OAuth2 JWT (Keycloak) |
| **Database** | PostgreSQL 15 + JPA/Hibernate 6 + Flyway migrations |
| **Cache** | Redis (Spring Cache) |
| **Messaging** | Apache Kafka |
| **API Style** | REST + GraphQL + gRPC |
| **Resilience** | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| **Tracing** | Micrometer + Zipkin (Brave) |
| **Metrics** | Prometheus + Grafana |
| **Logs** | Logstash JSON encoder + Loki + Promtail |
| **Testing** | JUnit 5, Spring Cloud Contract, Testcontainers, EmbeddedKafka |
| **Container** | Kubernetes (Skaffold), minikube/k3s |

---

## Patterns Implemented

### 1. Event Sourcing (order-service)

Events are the source of truth. Every state change is stored as an immutable event in `order_events` table.

**Files:**
- `domain/OrderEventRecord.kt` — JPA entity with JSONB payload
- `event/OrderEventStore.kt` — append events, replay aggregates
- `event/OrderEvent.kt` — sealed class with `OrderCreatedEvent`, `OrderStatusChangedEvent`, `OrderCancelledEvent`
- `db/migration/V2__Add_order_events.sql` — schema with sequence_number for ordering

**Flow:**
```
createOrder() ──► save Order ──► eventStore.append(OrderCreatedEvent)
                                       │
                                       ▼
                                  order_events table (JSONB)
```

### 2. Outbox Pattern (order-service)

Eliminates dual-write race condition between DB and Kafka. Events written transactionally to DB, async publisher relays to Kafka.

**Files:**
- `event/OutboxPublisher.kt` — `@Scheduled` job (1s interval)
- `config/SchedulingConfig.kt` — enables scheduling

**Flow:**
```
1. Transaction: save Order + insert event_record(published=false)
2. @Scheduled job: SELECT WHERE published=false (PESSIMISTIC_WRITE lock)
3. Send to Kafka, await ACK with .get()
4. Mark published=true, set published_at
5. On failure: leave published=false, retry next cycle
```

**Guarantees:** at-least-once delivery, no duplicates if consumer is idempotent.

### 3. CQRS — Command Query Responsibility Segregation (order-service)

Write side stores events; read side maintains denormalized projection for fast queries.

**Files:**
- `domain/OrderProjection.kt` — read model entity
- `repository/OrderProjectionRepository.kt` — query API
- `event/OrderProjectionHandler.kt` — applies events to projection
- `event/OrderProjectionSubscriber.kt` — `@Scheduled` poller (500ms)
- `db/migration/V3__Add_order_projections.sql` — denormalized read table

**Architecture:**
```
Write Side                    Read Side
──────────                    ─────────
OrderService                  OrderProjectionRepository
   │                                │
   ▼                                ▼
order_events ──► Subscriber ──► order_projections
(events)         (event           (snapshots)
                  handler)
```

### 4. Consumer-Driven Contract Testing (order-service ↔ inventory-service)

Real CDC: producer publishes stub jar to Maven, consumer pulls it and verifies its assumptions against producer's actual contracts.

**Producer side (order-service):**
- `src/test/resources/contracts/order/*.groovy` — Spring Cloud Contract DSL
- `ContractVerifierBase.kt` — base class with trigger methods
- `build.gradle` — `verifierStubsJar` task + `publishToMavenLocal`

**Consumer side (inventory-service):**
- `RealCdcStubRunnerTest.kt` — uses `@AutoConfigureStubRunner` to pull producer stubs
- Reads producer's groovy contracts from stub jar
- Verifies fields exist before sending to Kafka
- If producer changes contract → consumer test fails

**Workflow:**
```
Producer                      Maven Local                Consumer
────────                      ───────────                ────────
1. Write contract.groovy
2. ./gradlew verifierStubsJar
3. publishToMavenLocal ───────► stubs.jar
                                    │
                                    ▼
                              4. @AutoConfigureStubRunner
                                 pulls latest
                                    │
                                    ▼
                              5. Reads contract from stubs.jar
                              6. Verifies expected fields
                              7. Sends event to Kafka
                              8. Listener receives → assert
```

### 5. API Composition Pattern (api-gateway)

Composes data from multiple downstream services in a single request. See [API_COMPOSITION_PATTERN.md](API_COMPOSITION_PATTERN.md).

### 6. Circuit Breaker (api-gateway, order-service)

Resilience4j prevents cascading failures. Configured in `application.yml` per downstream:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

### 7. Distributed Tracing

Brave + Micrometer auto-instruments REST, Kafka, JDBC. Every request has `traceId` propagated through services. Visible in Zipkin UI.

---

## Services

| Service | Port | Responsibility |
|---------|------|----------------|
| `api-gateway` | 8080 | JWT validation, routing, rate limiting, circuit breaker, API composition |
| `order-service` | 8081 (HTTP) / 9090 (gRPC) | Create/manage orders, event sourcing, CQRS, Kafka publisher |
| `inventory-service` | 8082 (HTTP) / 9090 (gRPC) | Stock management, Kafka consumer, gRPC server |
| `keycloak` | 8080 | OAuth2/JWT issuer |
| `zipkin` | 9411 | Distributed tracing UI |
| `prometheus` | 9090 | Metrics collection |
| `grafana` | 3000 | Dashboards |
| `loki` | 3100 | Log aggregation |

---

## Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21+ |
| Docker Desktop | 20+ |
| Skaffold | 2.0+ |
| minikube / kind | 1.28+ |
| kubectl | 1.28+ |

### Option A — Full Stack via Skaffold (Recommended)

Builds all services and deploys to local Kubernetes.

```bash
# 1. Start minikube
minikube start --cpus=4 --memory=8192

# 2. Lightweight services-only profile (faster, no observability stack)
skaffold dev -p lightweight-services-only

# OR full stack with observability
skaffold dev
```

Services start automatically. Check status:
```bash
kubectl get pods -n microservices
kubectl logs -n microservices -l app=order-service --tail=50
```

### Option B — Infra in Docker, Services in IDE (Active Development)

Run dependencies in Docker, services from IDE for hot reload.

```bash
# Start infra only
skaffold dev -p lightweight-infra-only
```

Then run each service from IDE with these env vars:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=orders
DB_USERNAME=postgres
DB_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/microservices
```

### Configure Keycloak

Open **http://localhost:8080** → login `admin / adminPass`.

1. **Create realm:** `microservices`
2. **Create client:** `microservices-client` (Client authentication: ON, copy secret)
3. **Create roles:** `USER`, `ADMIN`
4. **Create user:** `testuser` with password, assign `USER` role

---

## API Usage

### Get JWT Token

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8080/realms/microservices/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=microservices-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=testuser" \
  -d "password=YOUR_PASSWORD" \
  | jq -r '.access_token')
```

### Order Service

```bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [{
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 2,
      "unitPrice": 999.99
    }]
  }'

# Get order
curl http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer $TOKEN"

# Update status (creates OrderStatusChangedEvent)
curl -X PUT "http://localhost:8080/api/orders/{id}/status?status=SHIPPED" \
  -H "Authorization: Bearer $TOKEN"

# Cancel order (creates OrderCancelledEvent)
curl -X DELETE http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer $TOKEN"

# Verify event sourcing — check order_events table
psql -h localhost -U postgres -d orders -c "SELECT event_type, sequence_number, published FROM order_events ORDER BY sequence_number;"

# Verify CQRS — check projection
psql -h localhost -U postgres -d orders -c "SELECT aggregate_id, status, last_event_sequence FROM order_projections;"
```

### Inventory Service

```bash
# Add item
curl -X POST http://localhost:8080/api/inventory \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD-001",
    "productName": "Laptop",
    "quantity": 100,
    "unitPrice": 999.99
  }'

# Reserve stock
curl -X POST http://localhost:8080/api/inventory/reserve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId": "PROD-001", "quantity": 5}'
```

### Order Status Lifecycle

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                          │
                          └────────────────► CANCELLED
```

---

## Testing Strategy

```
        ┌─────────────────────────┐
        │   E2E / Integration     │  EventSourcingIntegrationTest
        │   (slow, real infra)    │  (Testcontainers + EmbeddedKafka)
        ├─────────────────────────┤
        │   Real CDC Tests        │  RealCdcStubRunnerTest
        │   (cross-service)       │  (pulls producer stubs)
        ├─────────────────────────┤
        │   Producer/Consumer CDC │  ProducerCdcTest, ConsumerCdcTest
        │   (per-side)            │  (real Kafka publish/consume)
        ├─────────────────────────┤
        │   Contract Tests        │  OrderEventContractTest
        │   (schema validation)   │  (manual JUnit, JSON schema)
        ├─────────────────────────┤
        │   Unit Tests            │  (service-level, with mocks)
        └─────────────────────────┘
```

### Run Tests

```bash
# All tests (excluding tests requiring Docker)
./gradlew test -x contractTest

# Producer CDC tests
cd order-service && ./gradlew test --tests "*ProducerCdcTest*"

# Consumer CDC tests (manual)
cd inventory-service && ./gradlew test --tests "*ConsumerCdcTest*"

# Real Cross-Service CDC (requires producer stub jar published)
cd order-service && ./gradlew verifierStubsJar publishToMavenLocal
cd inventory-service && ./gradlew test --tests "*RealCdcStubRunnerTest*"

# Generated contract tests (Spring Cloud Contract)
cd order-service && ./gradlew contractTest
```

### Contract Testing Workflow

**Producer changes contract:**
```bash
# 1. Modify groovy contract (e.g., add new field)
vi order-service/src/test/resources/contracts/order/order_created.groovy

# 2. Regenerate and publish stub jar
cd order-service
./gradlew verifierStubsJar publishToMavenLocal

# 3. Consumer test detects breaking change
cd ../inventory-service
./gradlew test --tests "*RealCdcStubRunnerTest*"
# → FAIL if consumer doesn't handle new schema
```

---

## Observability

### Distributed Tracing — Zipkin

```bash
kubectl port-forward -n microservices svc/zipkin 9411:9411
open http://localhost:9411
```

Search by `traceId` from logs to follow a request across all services.

### Metrics — Prometheus + Grafana

```bash
kubectl port-forward -n microservices svc/grafana 3000:3000
open http://localhost:3000  # admin/admin
```

Pre-configured dashboards:
- HTTP request latency (p50/p95/p99)
- Kafka consumer lag
- Circuit breaker state
- Custom: `orders.created.total`, `orders.cancelled.total`, `orders.creation.duration`

### Logs — Loki + Promtail

```bash
# Query in Grafana → Explore → Loki
{app="order-service"} |= "Order created"
```

All services emit structured JSON logs with `traceId` and `spanId`.

### Health Endpoints

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/circuitbreakers
curl http://localhost:8081/actuator/prometheus
```

---

## Project Structure

```
microservices-demo/
├── k8s/                          # Shared infrastructure manifests
│   ├── namespace.yaml
│   ├── postgres.yaml
│   ├── redis.yaml
│   ├── kafka.yaml
│   ├── keycloak.yaml
│   ├── zipkin.yaml
│   ├── prometheus.yaml
│   ├── grafana.yaml
│   └── loki.yaml
│
├── api-gateway/                  # WebFlux gateway
│   ├── build.gradle
│   ├── k8s/
│   └── src/main/kotlin/uz/coder/api_gateway/
│       ├── config/               # Security, routing, circuit breaker
│       ├── filter/               # Logging, JWT propagation
│       └── service/              # API composition
│
├── order-service/                # Event sourcing + CQRS
│   ├── build.gradle              # Includes Spring Cloud Contract plugin
│   ├── k8s/
│   ├── src/main/kotlin/uz/coder/order_service/
│   │   ├── domain/               # Order, OrderItem, OrderEventRecord, OrderProjection
│   │   ├── dto/                  # OrderRequest, OrderResponse
│   │   ├── repository/           # OrderRepository, EventStoreRepository, OrderProjectionRepository
│   │   ├── service/              # OrderService (commands)
│   │   ├── controller/           # REST + GraphQL
│   │   ├── event/                # Event sourcing components
│   │   │   ├── OrderEvent.kt
│   │   │   ├── OrderEventType.kt
│   │   │   ├── OrderEventStore.kt
│   │   │   ├── OutboxPublisher.kt
│   │   │   ├── OrderProjectionHandler.kt
│   │   │   └── OrderProjectionSubscriber.kt
│   │   ├── audit/                # AOP audit logging
│   │   ├── config/               # Caching, scheduling, gRPC
│   │   └── exception/            # Global error handling
│   └── src/test/
│       ├── kotlin/.../contract/  # CDC tests
│       │   ├── OrderEventContractTest.kt
│       │   ├── ProducerCdcTest.kt
│       │   └── ContractVerifierBase.kt
│       └── resources/contracts/  # Groovy contract DSL
│           └── order/
│               ├── order_created.groovy
│               ├── order_status_changed.groovy
│               └── order_cancelled.groovy
│
└── inventory-service/            # Kafka consumer + stock mgmt
    ├── build.gradle
    ├── k8s/
    └── src/
        ├── main/kotlin/uz/coder/inventory_service/
        │   ├── domain/
        │   ├── service/
        │   ├── controller/
        │   ├── event/            # InventoryEventPublisher, OrderEventListener
        │   └── grpc/             # gRPC server
        └── test/kotlin/.../contract/
            ├── OrderEventContractTest.kt
            ├── ConsumerCdcTest.kt
            └── RealCdcStubRunnerTest.kt   # ← Real CDC via stub runner
```

---

## Configuration Reference

All services read config from environment variables (12-factor). Defaults shown.

| Variable | Used By | Default |
|----------|---------|---------|
| `DB_HOST` | order, inventory | `localhost` |
| `DB_PORT` | order, inventory | `5432` |
| `DB_NAME` | order, inventory | `orders` / `inventory` |
| `DB_USERNAME` | order, inventory | `postgres` |
| `DB_PASSWORD` | order, inventory | `postgres` |
| `REDIS_HOST` | all | `redis` |
| `REDIS_PORT` | all | `6379` |
| `REDIS_PASSWORD` | all | `redisPass` |
| `KAFKA_BOOTSTRAP_SERVERS` | order, inventory | `kafka:9092` |
| `KEYCLOAK_ISSUER_URI` | all | `http://keycloak:8080/realms/microservices` |
| `INVENTORY_SERVICE_URL` | order | `http://inventory-service:8082` |

---

## Troubleshooting

### `401 Unauthorized` on all requests
Check JWT token isn't expired (paste at jwt.io). Verify `KEYCLOAK_ISSUER_URI` matches realm URL exactly.

### `503 Service Unavailable` from gateway
Circuit breaker open. Check `/actuator/circuitbreakers` on target. Wait `waitDurationInOpenState` (10s).

### Flyway migration fails
Database (`orders` or `inventory`) must exist before service starts. Check `k8s/postgres.yaml` init scripts.

### `Unable to connect to Kafka`
In K8s use `kafka:9092`. Locally use `localhost:9092`. For Docker Compose use `kafka:29092` (internal listener).

### JSONB column error: "type jsonb but expression is of type character varying"
Hibernate needs `@Type(JsonType::class)` from `hypersistence-utils-hibernate-60`. Already configured on `OrderEventRecord.payload`.

### Testcontainers `Could not find valid Docker environment`
Docker daemon issue. Verify with `docker ps`. On macOS, set:
```bash
echo "docker.host=unix:///Users/$USER/.docker/run/docker.sock" > ~/.testcontainers.properties
```

### Spring Cloud Contract `Plugin not found`
The plugin uses `buildscript` block with classpath, not the standard plugins block:
```gradle
buildscript {
    dependencies {
        classpath 'org.springframework.cloud:spring-cloud-contract-gradle-plugin:4.1.3'
    }
}
apply plugin: 'spring-cloud-contract'
```

### Real CDC test fails: "Producer stub jar not found"
Producer must publish stub jar first:
```bash
cd order-service && ./gradlew verifierStubsJar publishToMavenLocal
```

---

## Patterns Reference

| Pattern | Where | Why |
|---------|-------|-----|
| Event Sourcing | order-service | Audit trail, time-travel debugging, immutable history |
| Outbox | order-service | Eliminates dual-write race between DB and Kafka |
| CQRS | order-service | Separate optimized read model from write model |
| Saga | (future) | Distributed transactions across services |
| Circuit Breaker | api-gateway, order-service | Prevents cascading failures |
| Bulkhead | (Resilience4j) | Resource isolation per downstream |
| API Composition | api-gateway | Aggregate data from multiple services in one call |
| Database per Service | order/inventory | Service autonomy, no shared schema |
| Strangler Fig | (future) | Gradual migration from monolith |
| Sidecar | (future Istio) | Cross-cutting concerns externalized |

---

## Contributing

1. Create feature branch from `main`
2. Follow existing code style (ktlint where applicable)
3. Add tests for new patterns
4. Update relevant section in this README
5. Open PR with description of pattern/feature

---

## License

MIT — see LICENSE file.