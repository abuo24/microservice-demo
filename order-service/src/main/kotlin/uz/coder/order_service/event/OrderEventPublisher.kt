package uz.coder.order_service.event

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class OrderEventPublisher(private val kafkaTemplate: KafkaTemplate<String, OrderEvent>) {

    fun publishOrderCreated(event: OrderCreatedEvent) {
        kafkaTemplate.send("order-created", event.orderId.toString(), event)
    }

    fun publishOrderStatusChanged(event: OrderStatusChangedEvent) {
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event)
    }
}