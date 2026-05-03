package uz.coder.inventory_service.dto

import uz.coder.inventory_service.domain.InventoryItem
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

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
) {
    companion object {
        fun from(item: InventoryItem) = InventoryResponse(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            quantity = item.quantity,
            reservedQuantity = item.reservedQuantity,
            availableQuantity = item.availableQuantity,
            unitPrice = item.unitPrice,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )
    }
}