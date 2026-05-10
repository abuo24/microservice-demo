package uz.coder.order_service.event

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class InventoryEventListener {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["stock-reserved"], groupId = "order-service")
    fun onStockReserved(message: String) {
        log.info("Stock reserved event received: {}", message)
    }

    @KafkaListener(topics = ["stock-released"], groupId = "order-service")
    fun onStockReleased(message: String) {
        log.info("Stock released event received: {}", message)
    }

    @KafkaListener(topics = ["inventory-updated"], groupId = "order-service")
    fun onInventoryUpdated(message: String) {
        log.info("Inventory updated event received: {}", message)
    }
}