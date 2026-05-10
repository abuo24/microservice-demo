package uz.coder.api_gateway.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val customerId: String,
    val totalAmount: BigDecimal,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val items: List<OrderItemResponse> = emptyList()
)

data class OrderItemResponse(
    val id: UUID,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal
)

data class InventoryResponse(
    val id: UUID,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val reservedQuantity: Int,
    val availableQuantity: Int,
    val unitPrice: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class OrderWithInventoryComposition(
    val order: OrderResponse,
    val inventory: InventoryResponse?,
    val composedAt: Instant = Instant.now()
)