package uz.coder.inventory_service.event

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class OrderEventListener {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["order-created"], groupId = "inventory-service")
    fun onOrderCreated(event: OrderCreatedEvent) {
        log.info("Order created event received: orderId={}, customerId={}, totalAmount={}",
            event.orderId, event.customerId, event.totalAmount)
    }

    @KafkaListener(topics = ["order-status-changed"], groupId = "inventory-service")
    fun onOrderStatusChanged(event: OrderStatusChangedEvent) {
        log.info("Order status changed event received: orderId={}, newStatus={}, previousStatus={}",
            event.orderId, event.newStatus, event.previousStatus)
    }
}