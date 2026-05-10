package uz.coder.inventory_service.event

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

sealed class OrderEvent {
    abstract val eventId: String
    abstract val timestamp: Instant
}

data class OrderCreatedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    val orderId: UUID,
    val customerId: String,
    val totalAmount: BigDecimal
) : OrderEvent()

data class OrderStatusChangedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    val orderId: UUID,
    val newStatus: String,
    val previousStatus: String
) : OrderEvent()

data class OrderCancelledEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    val orderId: UUID,
    val previousStatus: String,
    val reason: String? = null
) : OrderEvent()