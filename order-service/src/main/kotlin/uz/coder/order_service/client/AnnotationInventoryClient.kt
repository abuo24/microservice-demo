package uz.coder.order_service.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import uz.coder.order_service.dto.StockCheckResponse

@Service
class AnnotationInventoryClient {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    @Value("\${inventory.service.url:http://inventory-service:8082}")
    private lateinit var inventoryUrl: String

    // CASE 3: annotation-based, real inventory-service — circuit stays CLOSED, returns live data
    @CircuitBreaker(name = "inventory-annotation", fallbackMethod = "stockFallback")
    fun checkStock(productId: String): StockCheckResponse {
        log.info("[@CircuitBreaker] Calling real inventory for product=$productId")
        val res = restClient.get()
            .uri("$inventoryUrl/api/inventory/product/$productId")
            .retrieve()
            .body(Map::class.java)
        val qty = (res?.get("availableQuantity") as? Number)?.toInt() ?: 0
        return StockCheckResponse(
            productId = productId,
            productName = res?.get("productName") as? String ?: productId,
            availableQuantity = qty,
            available = qty > 0,
            fallback = false
        )
    }

    // CASE 4: annotation-based, non-existent host — every call fails, circuit opens, fallback runs
    @CircuitBreaker(name = "inventory-annotation-broken", fallbackMethod = "stockFallback")
    fun checkStockBroken(productId: String): StockCheckResponse {
        log.info("[@CircuitBreaker] Calling broken endpoint for product=$productId")
        restClient.get()
            .uri("http://non-existent-inventory:9999/product/$productId")
            .retrieve()
            .body(Map::class.java)
        return StockCheckResponse(productId, "Unreachable", 0, false)
    }

    // fallback — same return type, extra Throwable param at end
    fun stockFallback(productId: String, ex: Throwable): StockCheckResponse {
        log.warn("[@CircuitBreaker] Fallback triggered for product=$productId: ${ex.javaClass.simpleName}")
        return StockCheckResponse(
            productId = productId,
            productName = "Unknown (annotation fallback)",
            availableQuantity = 0,
            available = false,
            fallback = true,
            fallbackReason = "${ex.javaClass.simpleName}: ${ex.message}"
        )
    }
}