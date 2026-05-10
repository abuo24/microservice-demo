package uz.coder.inventory_service.event

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class InventoryEventPublisher(private val kafkaTemplate: KafkaTemplate<String, InventoryEvent>) {

    fun publishStockReserved(event: StockReservedEvent) {
        kafkaTemplate.send("stock-reserved", event.productId, event)
    }

    fun publishStockReleased(event: StockReleasedEvent) {
        kafkaTemplate.send("stock-released", event.productId, event)
    }

    fun publishInventoryUpdated(event: InventoryUpdatedEvent) {
        kafkaTemplate.send("inventory-updated", event.productId, event)
    }
}