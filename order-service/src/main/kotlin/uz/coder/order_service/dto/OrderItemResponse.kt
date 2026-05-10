package uz.coder.order_service.dto

import uz.coder.order_service.domain.OrderItem
import java.math.BigDecimal
import java.util.UUID

data class OrderItemResponse(
    val id: UUID,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal
) {
    companion object {
        fun from(item: OrderItem) = OrderItemResponse(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            quantity = item.quantity,
            unitPrice = item.unitPrice
        )
    }
}