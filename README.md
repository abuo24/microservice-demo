# Microservices Demo

A production-grade microservices architecture built with **Kotlin + Spring Boot 4 + Kubernetes**.

## Architecture Overview

```
                        ┌─────────────────────────────────────┐
                        │           Kubernetes Cluster         │
                        │                                      │
Internet ──► Ingress ──►│  api-gateway :8080                   │
                        │      │                               │
                        │      ├──► order-service :8081        │
                        │      │         │                     │
                        │      └──► inventory-service :8082    │
                        │                │                     │
                        │         ┌──────┼──────────┐          │
                        │       Kafka  Redis  PostgreSQL       │
                        │                                      │
                        │    Keycloak  Zipkin                  │
                        └─────────────────────────────────────┘
```

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8080 | JWT validation, routing, rate limiting, circuit breaker |
| `order-service` | 8081 | Create/manage orders, publish Kafka events |
| `inventory-service` | 8082 | Stock management, consume Kafka order events |
| `keycloak` | 8080 (K8s) | OAuth2/JWT issuer |
| `zipkin` | 9411 | Distributed tracing UI |

## Tech Stack

- **Language:** Kotlin 2.2
- **Framework:** Spring Boot 4.0 (WebMVC for services, WebFlux for gateway)
- **Build:** Gradle (Groovy DSL)
- **Security:** Spring Security + OAuth2 JWT (Keycloak)
- **Database:** PostgreSQL + JPA/Hibernate + Flyway migrations
- **Cache:** Redis
- **Messaging:** Apache Kafka
- **Circuit Breaker:** Resilience4j
- **Tracing:** Micrometer + Zipkin (Brave)
- **Logging:** Logstash JSON (structured)
- **Kubernetes:** Spring Cloud Kubernetes Fabric8, RBAC, HPA, Ingress

---

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 24+ |
| Gradle | via wrapper (`./gradlew`) |
| Docker | 20+ |
| kubectl | 1.28+ |
| A Kubernetes cluster | minikube / kind / k3s / cloud |

---

## Quick Start — Local Development

There are two ways to run locally. Docker Compose is recommended — **do not** try to use the `k8s/` manifests locally unless you have minikube/kind set up.

---

### Option A — Full Stack (everything in Docker)

Builds all three services and runs the complete system with one command.

```bash
# 1. Build all service images
docker compose build

# 2. Start everything
docker compose up -d

# 3. Watch logs
docker compose logs -f
```

Services start in this order automatically (health checks enforce it):
`postgres` → `redis + kafka` → `keycloak` → `order-service + inventory-service` → `api-gateway`

> **First startup takes ~3–5 minutes** — Keycloak needs to initialize its database.

After startup, set up the Keycloak realm (see [Configure Keycloak](#configure-keycloak) below), then [use the API](#using-the-api).

```bash
# Stop and remove containers (keep volumes/data)
docker compose down

# Stop and wipe all data
docker compose down -v
```

---

### Option B — Infra in Docker, Services in IDE

Best for active development — run Postgres/Redis/Kafka/Keycloak in Docker, run each service from IntelliJ/terminal with hot reload.

**Step 1 — Start infrastructure only:**
```bash
docker compose -f docker-compose.infra.yml up -d
```

Exposed ports on `localhost`:
| Service | Port |
|---|---|
| PostgreSQL | `5432` |
| Redis | `6379` |
| Kafka | `9094` ← use this from your machine (not 9092) |
| Zipkin UI | `9411` |
| Keycloak Admin | `8090` |

**Step 2 — Set up Keycloak realm** (see [Configure Keycloak](#configure-keycloak) below, using http://localhost:8090)

**Step 3 — Run services** from your IDE or terminal with these env vars:

**order-service** (run config / terminal export):
```bash
SPRING_PROFILES_ACTIVE=local,docker
DB_HOST=localhost
DB_PORT=5432
DB_NAME=orders
DB_USERNAME=postgres
DB_PASSWORD=dbPass
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redisPass
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
JWT_ISSUER_URI=http://localhost:8090/realms/microservices
JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs
```

**inventory-service:**
```bash
SPRING_PROFILES_ACTIVE=local,docker
DB_HOST=localhost
DB_PORT=5432
DB_NAME=inventory
DB_USERNAME=postgres
DB_PASSWORD=dbPass
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redisPass
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
JWT_ISSUER_URI=http://localhost:8090/realms/microservices
JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs
```

**api-gateway** (add extra flags to disable K8s):
```bash
SPRING_PROFILES_ACTIVE=local,docker
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redisPass
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
JWT_ISSUER_URI=http://localhost:8090/realms/microservices
JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs
# Override routes to point at local ports
SPRING_CLOUD_GATEWAY_ROUTES[0]_ID=order-service
SPRING_CLOUD_GATEWAY_ROUTES[0]_URI=http://localhost:8081
SPRING_CLOUD_GATEWAY_ROUTES[0]_PREDICATES[0]=Path=/api/orders/**
SPRING_CLOUD_GATEWAY_ROUTES[1]_ID=inventory-service
SPRING_CLOUD_GATEWAY_ROUTES[1]_URI=http://localhost:8082
SPRING_CLOUD_GATEWAY_ROUTES[1]_PREDICATES[0]=Path=/api/inventory/**
```

Then run each service:
```bash
cd order-service && ./gradlew bootRun
cd inventory-service && ./gradlew bootRun
cd api-gateway && ./gradlew bootRun
```

---

### Configure Keycloak

Open **http://localhost:8090** → login `admin / adminPass`.

1. **Create realm:** "Create realm" → name: `microservices` → Save
2. **Create client:**
   - Clients → Create client
   - Client ID: `microservices-client`
   - Client authentication: **ON** → Save
   - Credentials tab → copy **Client Secret**
3. **Create roles:** Realm roles → `USER`, `ADMIN`
4. **Create test user:**
   - Users → Add user → username: `testuser` → Save
   - Credentials → Set password (turn off Temporary)
   - Role Mappings → assign `USER`

---

### 1. Start infrastructure with Docker (manual, alternative to compose)

If you prefer individual `docker run` commands, see the full commands in the previous version — but Docker Compose above is simpler.

### 2. Configure Keycloak

Open **http://localhost:8090** → login with `admin / adminPass`.

1. **Create Realm:** click "Create realm" → name it `microservices` → Save
2. **Create Client:**
   - Clients → Create client
   - Client ID: `microservices-client`
   - Client authentication: ON
   - Authorization: ON → Save
   - Credentials tab → copy the **Client Secret**
3. **Add Roles:**
   - Realm roles → Create role → `USER` → Save
   - Create role → `ADMIN` → Save
4. **Create a test user:**
   - Users → Add user → Username: `testuser` → Save
   - Credentials tab → Set password (disable "Temporary")
   - Role Mappings → Assign `USER` role

### 3. Run the services

Each service needs these environment variables. Set them in your IDE run config or export in terminal:

**order-service:**
```bash
cd order-service

export DB_HOST=localhost DB_PORT=5432 DB_NAME=orders
export DB_USERNAME=postgres DB_PASSWORD=dbPass
export REDIS_HOST=localhost REDIS_PORT=6379 REDIS_PASSWORD=redisPass
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
export JWT_ISSUER_URI=http://localhost:8090/realms/microservices
export JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs
export SPRING_PROFILES_ACTIVE=local

./gradlew bootRun
```

**inventory-service:**
```bash
cd inventory-service

export DB_HOST=localhost DB_PORT=5432 DB_NAME=inventory
export DB_USERNAME=postgres DB_PASSWORD=dbPass
export REDIS_HOST=localhost REDIS_PORT=6379 REDIS_PASSWORD=redisPass
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
export JWT_ISSUER_URI=http://localhost:8090/realms/microservices
export JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs
export SPRING_PROFILES_ACTIVE=local

./gradlew bootRun
```

**api-gateway** (disable Kubernetes discovery locally):
```bash
cd api-gateway

export REDIS_HOST=localhost REDIS_PORT=6379 REDIS_PASSWORD=redisPass
export ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
export JWT_ISSUER_URI=http://localhost:8090/realms/microservices
export JWT_JWK_SET_URI=http://localhost:8090/realms/microservices/protocol/openid-connect/certs
export SPRING_CLOUD_KUBERNETES_ENABLED=false
export SPRING_PROFILES_ACTIVE=local

./gradlew bootRun
```

> **Note:** When running locally without Kubernetes, configure static routes in api-gateway by adding to your run config:
> ```
> SPRING_CLOUD_GATEWAY_ROUTES_0_URI=http://localhost:8081
> SPRING_CLOUD_GATEWAY_ROUTES_1_URI=http://localhost:8082
> ```

---

## Deploy to Kubernetes

### 1. Create namespace and shared infrastructure

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/zipkin.yaml
kubectl apply -f k8s/keycloak.yaml

# Wait for postgres and keycloak to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n microservices --timeout=120s
kubectl wait --for=condition=ready pod -l app=keycloak -n microservices --timeout=180s
```

### 2. Set up Keycloak realm in Kubernetes

```bash
# Port-forward Keycloak
kubectl port-forward -n microservices svc/keycloak 8090:8080

# Follow the same Keycloak setup steps from the local section above
# using http://localhost:8090
```

### 3. Build and push images

```bash
# Build each service (replace YOUR_REGISTRY with your registry)
cd api-gateway && ./gradlew bootBuildImage --imageName=YOUR_REGISTRY/api-gateway:0.0.1-SNAPSHOT
cd ../order-service && ./gradlew bootBuildImage --imageName=YOUR_REGISTRY/order-service:0.0.1-SNAPSHOT
cd ../inventory-service && ./gradlew bootBuildImage --imageName=YOUR_REGISTRY/inventory-service:0.0.1-SNAPSHOT

docker push YOUR_REGISTRY/api-gateway:0.0.1-SNAPSHOT
docker push YOUR_REGISTRY/order-service:0.0.1-SNAPSHOT
docker push YOUR_REGISTRY/inventory-service:0.0.1-SNAPSHOT
```

Update the `image:` field in each `k8s/deployment.yaml` to match your registry.

### 4. Deploy services

```bash
kubectl apply -f api-gateway/k8s/
kubectl apply -f order-service/k8s/
kubectl apply -f inventory-service/k8s/

# Verify all pods are running
kubectl get pods -n microservices
```

### 5. Verify

```bash
# Check pod status
kubectl get pods -n microservices

# Check logs
kubectl logs -n microservices -l app=order-service --tail=50

# Check circuit breaker health
kubectl port-forward -n microservices svc/order-service 8081:8081
curl http://localhost:8081/actuator/health
```

---

## Using the API

### Step 1 — Get a JWT token from Keycloak

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8090/realms/microservices/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=microservices-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=testuser" \
  -d "password=YOUR_PASSWORD" \
  | jq -r '.access_token')

echo $TOKEN
```

### Step 2 — Call the APIs

All requests go through the **api-gateway on port 8080**.

#### Inventory API

```bash
# Add inventory item (requires ADMIN role)
curl -X POST http://localhost:8080/api/inventory \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "PROD-001",
    "productName": "Laptop",
    "quantity": 100,
    "unitPrice": 999.99
  }'

# Check stock
curl http://localhost:8080/api/inventory/product/PROD-001 \
  -H "Authorization: Bearer $TOKEN"

# List all in-stock items
curl http://localhost:8080/api/inventory/in-stock \
  -H "Authorization: Bearer $TOKEN"

# Reserve stock
curl -X POST http://localhost:8080/api/inventory/reserve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId": "PROD-001", "quantity": 5}'
```

#### Order API

```bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop",
        "quantity": 2,
        "unitPrice": 999.99
      }
    ]
  }'

# Get order by ID
curl http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer $TOKEN"

# Get orders by customer
curl http://localhost:8080/api/orders/customer/customer-123 \
  -H "Authorization: Bearer $TOKEN"

# Update order status (requires ADMIN role)
curl -X PATCH "http://localhost:8080/api/orders/{id}/status?status=CONFIRMED" \
  -H "Authorization: Bearer $TOKEN"

# Cancel order
curl -X DELETE http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer $TOKEN"
```

#### Order Status Values
`PENDING` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED` / `CANCELLED`

---

## Observability

### Distributed Tracing — Zipkin

```bash
# Local
open http://localhost:9411

# Kubernetes
kubectl port-forward -n microservices svc/zipkin 9411:9411
open http://localhost:9411
```

Every request across gateway → service is traced. Search by `traceId` from response logs.

### Health & Metrics — Actuator

```bash
# Health (includes circuit breaker state)
curl http://localhost:8081/actuator/health

# Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Circuit breaker states
curl http://localhost:8081/actuator/circuitbreakers
```

### Structured Logs

In non-`local` profiles, all services output **JSON logs** with `traceId` and `spanId` fields — ready for Elasticsearch/Loki ingestion.

```json
{
  "timestamp": "2026-05-03T10:00:00.000Z",
  "level": "INFO",
  "service": "order-service",
  "traceId": "4bf92f3577b34da6",
  "spanId": "00f067aa0ba902b7",
  "message": "Order created id=abc-123"
}
```

---

## Project Structure

```
microservices-demo/
├── k8s/                          # Shared infrastructure
│   ├── namespace.yaml
│   ├── postgres.yaml
│   ├── redis.yaml
│   ├── kafka.yaml
│   ├── zipkin.yaml
│   └── keycloak.yaml
│
├── api-gateway/
│   ├── build.gradle
│   ├── k8s/                      # Gateway K8s manifests + Ingress
│   └── src/main/kotlin/uz/coder/api_gateway/
│       ├── config/
│       │   ├── SecurityConfig.kt         # WebFlux JWT auth
│       │   └── FallbackController.kt     # Circuit breaker fallbacks
│       └── filter/
│           └── LoggingGlobalFilter.kt    # Request/response logging
│
├── order-service/
│   ├── build.gradle
│   ├── k8s/
│   └── src/main/kotlin/uz/coder/order_service/
│       ├── domain/       # Order, OrderItem (JPA entities)
│       ├── dto/          # OrderRequest (validated), OrderResponse
│       ├── repository/   # OrderRepository (Spring Data JPA)
│       ├── service/      # OrderService (@CircuitBreaker, @Cacheable)
│       ├── controller/   # OrderController (@PreAuthorize)
│       ├── messaging/    # OrderEventPublisher (Kafka)
│       ├── config/       # SecurityConfig, CacheConfig
│       └── exception/    # GlobalExceptionHandler, OrderNotFoundException
│
└── inventory-service/
    ├── build.gradle
    ├── k8s/
    └── src/main/kotlin/uz/coder/inventory_service/
        ├── domain/       # InventoryItem (JPA entity, pessimistic lock)
        ├── dto/          # InventoryRequest, InventoryResponse, ReserveRequest
        ├── repository/   # InventoryRepository (with @Lock for reservations)
        ├── service/      # InventoryService (reserve/release/confirm)
        ├── controller/   # InventoryController
        ├── messaging/    # InventoryEventConsumer (Kafka listener)
        ├── config/       # SecurityConfig, CacheConfig
        └── exception/    # GlobalExceptionHandler, InsufficientStockException
```

---

## Configuration Reference

All services read config from **environment variables** (12-factor). In Kubernetes, these come from ConfigMaps and Secrets.

| Variable | Used By | Default |
|---|---|---|
| `DB_HOST` | order, inventory | `localhost` |
| `DB_PORT` | order, inventory | `5432` |
| `DB_NAME` | order, inventory | `orders` / `inventory` |
| `DB_USERNAME` | order, inventory | `postgres` |
| `DB_PASSWORD` | order, inventory | — |
| `REDIS_HOST` | all | `localhost` / `redis` |
| `REDIS_PASSWORD` | all | — |
| `KAFKA_BOOTSTRAP_SERVERS` | order, inventory | `localhost:9092` |
| `JWT_ISSUER_URI` | all | `http://keycloak:8080/realms/microservices` |
| `JWT_JWK_SET_URI` | all | `http://keycloak:8080/realms/microservices/...` |
| `ZIPKIN_ENDPOINT` | all | `http://zipkin:9411/api/v2/spans` |

---

## Common Issues

**`401 Unauthorized` on all requests**
→ Check your JWT token is not expired (`jwt.io` to inspect). Verify `JWT_ISSUER_URI` matches the Keycloak realm URL exactly.

**`503 Service Unavailable` from gateway**
→ Circuit breaker is open. Check `/actuator/circuitbreakers` on the target service. Wait for the `waitDurationInOpenState` (10s) to expire.

**Flyway migration fails on startup**
→ Ensure the database (`orders` or `inventory`) exists in PostgreSQL before starting the service.

**`Unable to connect to Kafka`**
→ Verify `KAFKA_BOOTSTRAP_SERVERS` is reachable. In K8s, use `kafka:9092`; locally use `localhost:9092`.

**Spring Cloud Kubernetes fails locally**
→ Set `SPRING_CLOUD_KUBERNETES_ENABLED=false` and `SPRING_PROFILES_ACTIVE=local` when running outside a cluster.