package uz.coder.order_service.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uz.coder.order_service.domain.Order
import uz.coder.order_service.domain.OrderEventRecord
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.repository.EventStoreRepository
import java.util.UUID

@Component
class OrderEventStore(
    private val eventStoreRepository: EventStoreRepository,
    private val objectMapper: ObjectMapper
) {

    fun append(event: OrderEvent): OrderEventRecord {
        val eventType = when (event) {
            is OrderCreatedEvent       -> OrderEventType.ORDER_CREATED
            is OrderStatusChangedEvent -> OrderEventType.ORDER_STATUS_CHANGED
            is OrderCancelledEvent     -> OrderEventType.ORDER_CANCELLED
        }
        val record = OrderEventRecord(
            aggregateId = event.orderId(),
            eventType   = eventType,
            payload     = objectMapper.writeValueAsString(event)
        )
        return eventStoreRepository.save(record)
    }

    fun replayOrder(orderId: UUID): Order? {
        val events = eventStoreRepository
            .findByAggregateIdOrderBySequenceNumberAsc(orderId)
        if (events.isEmpty()) return null
        return applyAll(events)
    }

    private fun applyAll(records: List<OrderEventRecord>): Order? {
        var order: Order? = null
        for (record in records) {
            order = applyOne(order, record)
        }
        return order
    }

    private fun applyOne(current: Order?, record: OrderEventRecord): Order? {
        return when (record.eventType) {
            OrderEventType.ORDER_CREATED -> {
                val e = objectMapper.readValue(record.payload, OrderCreatedEvent::class.java)
                Order(
                    id          = e.orderId,
                    customerId  = e.customerId,
                    status      = OrderStatus.PENDING,
                    totalAmount = e.totalAmount
                )
            }
            OrderEventType.ORDER_STATUS_CHANGED -> {
                val e = objectMapper.readValue(record.payload, OrderStatusChangedEvent::class.java)
                current!!.apply { status = OrderStatus.valueOf(e.newStatus) }
            }
            OrderEventType.ORDER_CANCELLED -> {
                current!!.apply { status = OrderStatus.CANCELLED }
            }
        }
    }
}

private fun OrderEvent.orderId(): UUID = when (this) {
    is OrderCreatedEvent       -> orderId
    is OrderStatusChangedEvent -> orderId
    is OrderCancelledEvent     -> orderId
}