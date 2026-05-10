package uz.coder.api_gateway.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import uz.coder.api_gateway.model.InventoryResponse
import uz.coder.api_gateway.model.OrderResponse
import uz.coder.api_gateway.model.OrderWithInventoryComposition
import java.util.UUID

@Service
class OrderCompositionService(private val webClient: WebClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "order-composition", fallbackMethod = "fallback")
    fun getOrderWithInventory(
        orderId: UUID,
        productId: UUID,
        exchange: ServerWebExchange
    ): Mono<OrderWithInventoryComposition> {
        log.info("Composing order {} with inventory data", orderId)

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: ""

        val order = fetchOrder(orderId, authHeader)
        val inventory = fetchInventory(productId, authHeader)

        return Mono.zip(order, inventory) { o, i ->
            OrderWithInventoryComposition(order = o, inventory = i)
        }
    }

    private fun fetchOrder(orderId: UUID, authHeader: String): Mono<OrderResponse> {
        log.debug("Fetching order from order-service: {}", orderId)
        return webClient.get()
            .uri("http://order-service:8081/api/orders/$orderId")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .retrieve()
            .bodyToMono(OrderResponse::class.java)
            .onErrorResume { ex ->
                log.error("Order fetch failed for {}: {}", orderId, ex.message)
                Mono.empty()
            }
    }

    private fun fetchInventory(orderId: UUID, authHeader: String): Mono<InventoryResponse?> {
        log.debug("Fetching inventory from inventory-service: {}", orderId)
        return webClient.get()
            .uri("http://inventory-service:8082/api/inventory/$orderId")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .retrieve()
            .bodyToMono(InventoryResponse::class.java)
            .onErrorResume { ex ->
                log.warn("Inventory fetch failed for order {}: {}", orderId, ex.message)
                Mono.empty()
            }
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallback(orderId: UUID, ex: Exception): Mono<OrderWithInventoryComposition> {
        log.error("Circuit breaker triggered for order composition: {}", ex.message)
        return Mono.error(RuntimeException("Service temporarily unavailable", ex))
    }
}