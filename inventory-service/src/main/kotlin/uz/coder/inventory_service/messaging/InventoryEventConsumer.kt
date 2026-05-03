package uz.coder.inventory_service.messaging

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uz.coder.inventory_service.service.InventoryService

@Component
class InventoryEventConsumer(private val inventoryService: InventoryService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["orders"], groupId = "inventory-service-group")
    fun handleOrderCreated(
        @Payload event: Map<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long
    ) {
        val eventType = event["eventType"] as? String ?: return
        val orderId = event["orderId"] as? String ?: return

        log.info("Received event type={} orderId={} topic={} partition={} offset={}",
            eventType, orderId, topic, partition, offset)

        when (eventType) {
            "ORDER_CREATED" -> processOrderCreated(event)
            "ORDER_STATUS_CHANGED" -> processOrderStatusChanged(event)
            else -> log.debug("Ignored unknown event type={}", eventType)
        }
    }

    private fun processOrderCreated(event: Map<String, Any>) {
        val orderId = event["orderId"] as? String ?: return
        log.info("Processing ORDER_CREATED orderId={}", orderId)
        // Saga pattern: reserve stock for the order items
        // In a real system, the event would include line items with productId + quantity
        // Here we log and acknowledge — actual reservation happens via REST from order-service
    }

    private fun processOrderStatusChanged(event: Map<String, Any>) {
        val orderId = event["orderId"] as? String ?: return
        val status = event["status"] as? String ?: return
        log.info("Processing ORDER_STATUS_CHANGED orderId={} status={}", orderId, status)

        when (status) {
            "CANCELLED" -> log.info("Order cancelled — releasing reservations for orderId={}", orderId)
            "DELIVERED" -> log.info("Order delivered — confirming deductions for orderId={}", orderId)
        }
    }
}