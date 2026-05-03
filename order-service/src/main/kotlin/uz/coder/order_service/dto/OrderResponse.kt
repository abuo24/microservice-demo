package uz.coder.order_service.dto

import uz.coder.order_service.domain.Order
import uz.coder.order_service.domain.OrderItem
import uz.coder.order_service.enumuration.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val customerId: String,
    val status: OrderStatus,
    val items: List<OrderItemResponse>,
    val totalAmount: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            id = order.id,
            customerId = order.customerId,
            status = order.status,
            items = order.items.map { OrderItemResponse.from(it) },
            totalAmount = order.totalAmount,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt
        )
    }
}

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