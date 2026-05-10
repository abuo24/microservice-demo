package uz.coder.order_service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uz.coder.order_service.domain.OrderEventRecord
import uz.coder.order_service.repository.EventStoreRepository
import java.time.Instant

@Component
class OutboxPublisher(
    private val eventStoreRepository: EventStoreRepository,
    private val kafkaTemplate: KafkaTemplate<String, OrderEvent>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publishPending() {
        val unpublished = eventStoreRepository.findUnpublishedEvents()
        if (unpublished.isEmpty()) return

        for (record in unpublished) {
            try {
                val (topic, event) = deserializeForKafka(record)
                kafkaTemplate.send(topic, record.aggregateId.toString(), event).get()
                record.published   = true
                record.publishedAt = Instant.now()
                eventStoreRepository.save(record)
                log.debug("Published event id={} type={} topic={}", record.id, record.eventType, topic)
            } catch (ex: Exception) {
                log.error("Failed to publish event id={} type={}", record.id, record.eventType, ex)
            }
        }
    }

    private fun deserializeForKafka(record: OrderEventRecord): Pair<String, OrderEvent> {
        return when (record.eventType) {
            OrderEventType.ORDER_CREATED -> {
                val e = objectMapper.readValue(record.payload, OrderCreatedEvent::class.java)
                "order-created" to e
            }
            OrderEventType.ORDER_STATUS_CHANGED -> {
                val e = objectMapper.readValue(record.payload, OrderStatusChangedEvent::class.java)
                "order-status-changed" to e
            }
            OrderEventType.ORDER_CANCELLED -> {
                val e = objectMapper.readValue(record.payload, OrderCancelledEvent::class.java)
                "order-status-changed" to e
            }
        }
    }
}