package uz.coder.order_service.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import uz.coder.order_service.dto.StockCheckResponse

@Component
class InventoryClient(
    private val circuitBreakerFactory: CircuitBreakerFactory<*, *>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    @Value("\${inventory.service.url:http://inventory-service:8082}")
    private lateinit var inventoryUrl: String

    // CASE 1: works — calls real inventory-service, returns live stock data
    fun checkStock(productId: String): StockCheckResponse {
        val cb = circuitBreakerFactory.create("inventory")
        return cb.run(
            {
                log.info("Calling inventory-service for product=$productId")
                val res = restClient.get()
                    .uri("$inventoryUrl/api/inventory/product/$productId")
                    .retrieve()
                    .body(Map::class.java)
                val qty = (res?.get("availableQuantity") as? Number)?.toInt() ?: 0
                StockCheckResponse(
                    productId = productId,
                    productName = res?.get("productName") as? String ?: productId,
                    availableQuantity = qty,
                    available = qty > 0,
                    fallback = false
                )
            },
            { ex ->
                log.warn("Circuit breaker fallback for inventory (working case): ${ex.message}")
                fallbackResponse(productId, ex.message)
            }
        )
    }

    // CASE 2: broken — calls non-existent host, circuit breaker opens after failures, returns fallback
    fun checkStockBroken(productId: String): StockCheckResponse {
        val cb = circuitBreakerFactory.create("inventory-broken")
        return cb.run(
            {
                log.info("Calling broken inventory endpoint for product=$productId")
                restClient.get()
                    .uri("http://non-existent-inventory:9999/product/$productId")
                    .retrieve()
                    .body(Map::class.java)
                StockCheckResponse(productId, "Unknown", 0, false)
            },
            { ex ->
                log.warn("Circuit breaker OPEN — broken endpoint fallback: ${ex.message}")
                fallbackResponse(productId, "Inventory service unreachable: ${ex.javaClass.simpleName}")
            }
        )
    }

    private fun fallbackResponse(productId: String, reason: String?) = StockCheckResponse(
        productId = productId,
        productName = "Unknown (fallback)",
        availableQuantity = 0,
        available = false,
        fallback = true,
        fallbackReason = reason
    )
}