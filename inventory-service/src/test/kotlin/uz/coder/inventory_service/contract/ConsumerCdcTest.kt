package uz.coder.inventory_service.contract

import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import uz.coder.inventory_service.event.OrderCancelledEvent
import uz.coder.inventory_service.event.OrderCreatedEvent
import uz.coder.inventory_service.event.OrderStatusChangedEvent
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import kotlin.test.assertTrue

@SpringBootTest(classes = [ConsumerCdcTest.TestEventCapture::class, uz.coder.inventory_service.InventoryServiceApplication::class])
@EmbeddedKafka(partitions = 1, topics = ["order-created", "order-status-changed"])
class ConsumerCdcTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var capturedEvents: TestEventCapture

    @Test
    fun `consumer can deserialize producer-shaped OrderCreatedEvent`() {
        capturedEvents.created.clear()

        val event = OrderCreatedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            orderId = UUID.randomUUID(),
            customerId = "cust-cdc-prod-1",
            totalAmount = BigDecimal("150.00")
        )
        kafkaTemplate.send("order-created", event.orderId.toString(), event)

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                assertTrue(capturedEvents.created.isNotEmpty(),
                    "Consumer must receive OrderCreatedEvent from producer-shaped message")
            }
    }

    @Test
    fun `consumer can deserialize producer-shaped OrderStatusChangedEvent`() {
        capturedEvents.statusChanged.clear()

        val event = OrderStatusChangedEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            orderId = UUID.randomUUID(),
            newStatus = "DELIVERED",
            previousStatus = "SHIPPED"
        )
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event)

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                assertTrue(capturedEvents.statusChanged.any { it.newStatus == "DELIVERED" },
                    "Consumer must receive OrderStatusChangedEvent")
            }
    }

    @Test
    fun `consumer can deserialize producer-shaped OrderCancelledEvent`() {
        capturedEvents.cancelled.clear()

        val event = OrderCancelledEvent(
            eventId = UUID.randomUUID().toString(),
            timestamp = Instant.now(),
            orderId = UUID.randomUUID(),
            previousStatus = "PENDING",
            reason = "Out of stock"
        )
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event)

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                assertTrue(capturedEvents.cancelled.any { it.previousStatus == "PENDING" },
                    "Consumer must receive OrderCancelledEvent")
            }
    }

    @TestConfiguration
    class TestEventCapture {
        val created: ConcurrentLinkedQueue<OrderCreatedEvent> = ConcurrentLinkedQueue()
        val statusChanged: ConcurrentLinkedQueue<OrderStatusChangedEvent> = ConcurrentLinkedQueue()
        val cancelled: ConcurrentLinkedQueue<OrderCancelledEvent> = ConcurrentLinkedQueue()

        @Bean
        @Primary
        fun captureListener(): EventCaptureListener = EventCaptureListener(created, statusChanged, cancelled)
    }

    @Component
    class EventCaptureListener(
        private val created: ConcurrentLinkedQueue<OrderCreatedEvent>,
        private val statusChanged: ConcurrentLinkedQueue<OrderStatusChangedEvent>,
        private val cancelled: ConcurrentLinkedQueue<OrderCancelledEvent>
    ) {
        @KafkaListener(topics = ["order-created"], groupId = "cdc-test-group", containerFactory = "kafkaListenerContainerFactory")
        fun onOrderCreated(event: OrderCreatedEvent) {
            created.add(event)
        }

        @KafkaListener(topics = ["order-status-changed"], groupId = "cdc-test-group-status", containerFactory = "kafkaListenerContainerFactory")
        fun onOrderStatusChanged(event: OrderStatusChangedEvent) {
            statusChanged.add(event)
        }

        @KafkaListener(topics = ["order-status-changed"], groupId = "cdc-test-group-cancelled", containerFactory = "kafkaListenerContainerFactory")
        fun onOrderCancelled(event: OrderCancelledEvent) {
            cancelled.add(event)
        }
    }
}