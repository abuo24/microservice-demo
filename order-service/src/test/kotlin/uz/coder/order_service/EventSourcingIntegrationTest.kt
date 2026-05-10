package uz.coder.order_service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import uz.coder.order_service.dto.OrderItemRequest
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.repository.EventStoreRepository
import uz.coder.order_service.repository.OrderProjectionRepository
import uz.coder.order_service.service.OrderService
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = ["log.segment.bytes=1048576"])
class EventSourcingIntegrationTest {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var eventStoreRepository: EventStoreRepository

    @Autowired
    private lateinit var projectionRepository: OrderProjectionRepository

    @Test
    fun testOrderCreationStoresEventAndUpdatesProjection() {
        val request = OrderRequest(
            customerId = "cust-123",
            items = listOf(
                OrderItemRequest("prod-1", "Product 1", 2, BigDecimal("50.00")),
                OrderItemRequest("prod-2", "Product 2", 1, BigDecimal("75.00"))
            )
        )

        val response = orderService.createOrder(request)
        assertNotNull(response.id)
        assertEquals(OrderStatus.PENDING, response.status)
        assertEquals(BigDecimal("175.00"), response.totalAmount)

        Thread.sleep(1000)

        val events = eventStoreRepository.findByAggregateIdOrderBySequenceNumberAsc(response.id!!)
        assertEquals(1, events.size)
        assertEquals("ORDER_CREATED", events[0].eventType.name)

        val projection = projectionRepository.findByAggregateId(response.id!!)
        assertNotNull(projection)
        assertEquals("cust-123", projection.customerId)
        assertEquals(OrderStatus.PENDING, projection.status)
        assertEquals(BigDecimal("175.00"), projection.totalAmount)
    }

    @Test
    fun testOrderStatusChangeCreatesEvent() {
        val request = OrderRequest(
            customerId = "cust-456",
            items = listOf(OrderItemRequest("prod-1", "Product 1", 1, BigDecimal("100.00")))
        )

        val created = orderService.createOrder(request)
        Thread.sleep(500)

        val updated = orderService.updateStatus(created.id!!, OrderStatus.SHIPPED)
        assertEquals(OrderStatus.SHIPPED, updated.status)

        Thread.sleep(500)

        val events = eventStoreRepository.findByAggregateIdOrderBySequenceNumberAsc(created.id!!)
        assertEquals(2, events.size)
        assertEquals("ORDER_CREATED", events[0].eventType.name)
        assertEquals("ORDER_STATUS_CHANGED", events[1].eventType.name)

        val projection = projectionRepository.findByAggregateId(created.id!!)
        assertNotNull(projection)
        assertEquals(OrderStatus.SHIPPED, projection.status)
    }

    @Test
    fun testOrderCancellationCreatesEvent() {
        val request = OrderRequest(
            customerId = "cust-789",
            items = listOf(OrderItemRequest("prod-1", "Product 1", 1, BigDecimal("50.00")))
        )

        val created = orderService.createOrder(request)
        Thread.sleep(500)

        val cancelled = orderService.cancelOrder(created.id!!)
        assertEquals(OrderStatus.CANCELLED, cancelled.status)

        Thread.sleep(500)

        val events = eventStoreRepository.findByAggregateIdOrderBySequenceNumberAsc(created.id!!)
        assertEquals(2, events.size)
        assertEquals("ORDER_CREATED", events[0].eventType.name)
        assertEquals("ORDER_CANCELLED", events[1].eventType.name)

        val projection = projectionRepository.findByAggregateId(created.id!!)
        assertNotNull(projection)
        assertEquals(OrderStatus.CANCELLED, projection.status)
    }
}