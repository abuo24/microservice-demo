package uz.coder.inventory_service.event

import java.time.Instant
import java.util.UUID

sealed class InventoryEvent {
    abstract val eventId: String
    abstract val timestamp: Instant
}

data class StockReservedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    val productId: String,
    val quantity: Int,
    val availableQuantity: Int
) : InventoryEvent()

data class StockReleasedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    val productId: String,
    val quantity: Int,
    val availableQuantity: Int
) : InventoryEvent()

data class InventoryUpdatedEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val timestamp: Instant = Instant.now(),
    val productId: String,
    val quantity: Int,
    val reservedQuantity: Int
) : InventoryEvent()