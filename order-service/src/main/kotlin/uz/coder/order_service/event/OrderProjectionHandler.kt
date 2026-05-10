package uz.coder.order_service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uz.coder.order_service.domain.OrderEventRecord
import uz.coder.order_service.domain.OrderProjection
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.repository.EventStoreRepository
import uz.coder.order_service.repository.OrderProjectionRepository
import uz.coder.order_service.repository.OrderRepository
import java.time.Instant
import java.util.UUID

@Component
class OrderProjectionHandler(
    private val eventStoreRepository: EventStoreRepository,
    private val projectionRepository: OrderProjectionRepository,
    private val orderRepository: OrderRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleEvent(orderId: UUID, record: OrderEventRecord) {
        try {
            when (record.eventType) {
                OrderEventType.ORDER_CREATED -> handleOrderCreated(record)
                OrderEventType.ORDER_STATUS_CHANGED -> handleOrderStatusChanged(record)
                OrderEventType.ORDER_CANCELLED -> handleOrderCancelled(record)
            }
            log.debug("Projection updated for orderId={} eventType={}", orderId, record.eventType)
        } catch (ex: Exception) {
            log.error("Failed to update projection for orderId={} eventType={}", orderId, record.eventType, ex)
        }
    }

    private fun handleOrderCreated(record: OrderEventRecord) {
        val event = objectMapper.readValue(record.payload, OrderCreatedEvent::class.java)
        val order = orderRepository.findById(event.orderId).orElse(null) ?: return

        val projection = OrderProjection(
            aggregateId = event.orderId,
            customerId = event.customerId,
            status = OrderStatus.PENDING,
            totalAmount = event.totalAmount,
            itemCount = 0,
            lastEventSequence = record.sequenceNumber,
            updatedAt = Instant.now()
        )
        projectionRepository.save(projection)
    }

    private fun handleOrderStatusChanged(record: OrderEventRecord) {
        val event = objectMapper.readValue(record.payload, OrderStatusChangedEvent::class.java)
        val projection = projectionRepository.findByAggregateId(event.orderId) ?: return

        projection.status = OrderStatus.valueOf(event.newStatus)
        projection.lastEventSequence = record.sequenceNumber
        projection.updatedAt = Instant.now()
        projectionRepository.save(projection)
    }

    private fun handleOrderCancelled(record: OrderEventRecord) {
        val event = objectMapper.readValue(record.payload, OrderCancelledEvent::class.java)
        val projection = projectionRepository.findByAggregateId(event.orderId) ?: return

        projection.status = OrderStatus.CANCELLED
        projection.lastEventSequence = record.sequenceNumber
        projection.updatedAt = Instant.now()
        projectionRepository.save(projection)
    }
}