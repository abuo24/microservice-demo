# API Composition Pattern

The API Composition pattern (also called Data Composition or Backend for Frontend - BFF) is a microservices pattern where a service (API Gateway) composes data from multiple downstream services and returns a unified response to the client.

## Problem Solved

In microservices, data is distributed across multiple services. Without composition, clients must:
1. Call order-service → get order details
2. Call inventory-service → get inventory status
3. Manually combine responses in client code

This creates tight coupling and increases network calls.

## Solution: API Composition

**API Gateway acts as composer:**
- Single endpoint: `/api/gateway/v1/composition/orders/{orderId}`
- Gateway calls order-service + inventory-service internally
- Returns unified response to client

### Diagram
```
Client
  |
  v
API Gateway (Composition Endpoint)
  |
  +---> Order Service (fetch order)
  |
  +---> Inventory Service (fetch inventory)
  |
  v
Unified Response
```

## Implementation

### Components

1. **Models** (`OrderWithInventory.kt`)
   - `OrderResponse`: Order details
   - `InventoryResponse`: Inventory details
   - `OrderWithInventoryComposition`: Unified response

2. **Service** (`OrderCompositionService.kt`)
   - Orchestrates calls to downstream services
   - Handles failures gracefully (inventory optional)
   - Circuit breaker for resilience

3. **Controller** (`CompositionController.kt`)
   - REST endpoint for composition
   - Swagger documentation

4. **Configuration** (`RestClientConfig.kt`)
   - RestTemplate bean with timeouts
   - Resilience4j circuit breaker

## API Usage

### Request
```bash
GET /api/gateway/v1/composition/orders/{orderId}
Authorization: Bearer <token>
```

### Response
```json
{
  "order": {
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "cust-123",
    "totalAmount": 150.00,
    "status": "PENDING",
    "createdAt": "2026-05-10T10:00:00Z"
  },
  "inventory": {
    "inventoryId": "inv-123",
    "productId": "prod-456",
    "quantity": 100,
    "reserved": 30,
    "available": 70
  },
  "composedAt": "2026-05-10T11:00:00Z"
}
```

## Resilience Features

### Circuit Breaker
- **Threshold**: 50% failure rate
- **Window**: 10 requests
- **Open timeout**: 10 seconds
- **States**: CLOSED → OPEN → HALF_OPEN → CLOSED

Configuration in `application.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      order-composition:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

### Graceful Degradation
- Order is required (exception if missing)
- Inventory is optional (null if unavailable)
- Prevents cascade failures

### Timeouts
- Connection timeout: 5 seconds
- Read timeout: 10 seconds

## Trade-offs

### Advantages
✅ Single call from client (reduced latency perception)
✅ Server-side composition (decouples client logic)
✅ Unified error handling
✅ Easy to add caching/versioning

### Disadvantages
❌ Synchronous composition (slower than parallel)
❌ Network latency from gateway to services
❌ Gateway becomes critical (SPOF if not HA)
❌ Tight coupling to downstream schemas

## Variations

### Async Composition
For non-critical data, use reactive streams:
```kotlin
fun getOrderWithInventoryAsync(orderId: UUID): Mono<OrderWithInventoryComposition> {
    val order = fetchOrderAsync(orderId)
    val inventory = fetchInventoryAsync(orderId)
    return Mono.zip(order, inventory) { o, i -> 
        OrderWithInventoryComposition(o, i)
    }
}
```

### Caching
Add Spring Cache to avoid repeated composition:
```kotlin
@Cacheable(value = "orders", key = "#orderId")
fun getOrderWithInventory(orderId: UUID): OrderWithInventoryComposition
```

### Event-Based
Subscribe to inventory changes instead of pulling:
- Order Service publishes `OrderCreated` event
- Inventory Service publishes `InventoryUpdated` event
- Cache side-by-side data for composition

## Testing

### Unit Test
```kotlin
@Test
fun testCompositionHappyPath() {
    val orderId = UUID.randomUUID()
    val result = compositionService.getOrderWithInventory(orderId)
    
    assertThat(result.order).isNotNull
    assertThat(result.inventory).isNotNull
}
```

### Integration Test
```kotlin
@Test
@WithMockUser
fun testCompositionEndpoint() {
    mockMvc.get("/api/gateway/v1/composition/orders/$orderId")
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.order").exists())
}
```

## Deployment

Ensure both downstream services are reachable:
```yaml
# Configure service discovery
spring.cloud.kubernetes.discovery.enabled: true
```

Or use explicit URLs (localhost for dev):
```
http://order-service:8081
http://inventory-service:8082
```

## Monitoring

Check health & circuit breaker status:
```bash
curl http://gateway:8080/actuator/health

# Circuit breaker details
curl http://gateway:8080/actuator/health/order-composition
```

Metrics:
- `resilience4j.circuitbreaker.calls.total`
- `resilience4j.circuitbreaker.state` (0=CLOSED, 1=OPEN, 2=HALF_OPEN)