package uz.coder.order_service.messaging

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import uz.coder.order_service.domain.Order

@Component
class OrderEventPublisher(private val kafkaTemplate: KafkaTemplate<String, Any>) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val ORDERS_TOPIC = "orders"
        const val ORDER_STATUS_TOPIC = "order-status"
    }

    fun publishOrderCreated(order: Order) {
        val event = mapOf(
            "eventType" to "ORDER_CREATED",
            "orderId" to order.id.toString(),
            "customerId" to order.customerId,
            "totalAmount" to order.totalAmount,
            "status" to order.status.name
        )
        kafkaTemplate.send(ORDERS_TOPIC, order.id.toString(), event)
            .thenAccept { result ->
                log.info(
                    "ORDER_CREATED published orderId={} partition={} offset={}",
                    order.id, result.recordMetadata.partition(), result.recordMetadata.offset()
                )
            }
            .exceptionally { ex ->
                log.error("Failed to publish ORDER_CREATED for orderId={}: {}", order.id, ex.message)
                null
            }
    }

    fun publishOrderStatusChanged(order: Order) {
        val event = mapOf(
            "eventType" to "ORDER_STATUS_CHANGED",
            "orderId" to order.id.toString(),
            "status" to order.status.name
        )
        kafkaTemplate.send(ORDER_STATUS_TOPIC, order.id.toString(), event)
            .thenAccept { _ -> log.info("ORDER_STATUS_CHANGED published orderId={}", order.id) }
            .exceptionally { ex ->
                log.error("Failed to publish ORDER_STATUS_CHANGED for orderId={}: {}", order.id, ex.message)
                null
            }
    }
}