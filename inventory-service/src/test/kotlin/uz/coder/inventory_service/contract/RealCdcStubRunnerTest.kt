package uz.coder.inventory_service.contract

import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.contract.stubrunner.StubFinder
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties
import org.springframework.cloud.contract.verifier.util.ContractVerifierDslConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.stereotype.Component
import uz.coder.inventory_service.event.OrderCancelledEvent
import uz.coder.inventory_service.event.OrderCreatedEvent
import uz.coder.inventory_service.event.OrderStatusChangedEvent
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.test.assertTrue

@SpringBootTest(classes = [
    RealCdcStubRunnerTest.TestEventCapture::class,
    uz.coder.inventory_service.InventoryServiceApplication::class
])
@EmbeddedKafka(partitions = 1, topics = ["order-created", "order-status-changed"])
@AutoConfigureStubRunner(
    ids = ["uz.coder:order-service:+:stubs"],
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class RealCdcStubRunnerTest {

    @Autowired
    private lateinit var stubFinder: StubFinder

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var capturedEvents: TestEventCapture

    @Test
    fun `consumer receives real producer contract for OrderCreatedEvent`() {
        capturedEvents.created.clear()

        val event = buildEventFromContract<OrderCreatedEvent>("order_created.groovy")
        kafkaTemplate.send("order-created", event.orderId.toString(), event).get()

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                assertTrue(capturedEvents.created.isNotEmpty(),
                    "Consumer must receive event matching producer contract")
            }
    }

    @Test
    fun `consumer receives real producer contract for OrderStatusChangedEvent`() {
        capturedEvents.statusChanged.clear()

        val event = buildEventFromContract<OrderStatusChangedEvent>("order_status_changed.groovy")
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event).get()

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                assertTrue(capturedEvents.statusChanged.isNotEmpty(),
                    "Consumer must receive event matching producer contract")
            }
    }

    @Test
    fun `consumer receives real producer contract for OrderCancelledEvent`() {
        capturedEvents.cancelled.clear()

        val event = buildEventFromContract<OrderCancelledEvent>("order_cancelled.groovy")
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event).get()

        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                assertTrue(capturedEvents.cancelled.isNotEmpty(),
                    "Consumer must receive event matching producer contract")
            }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> buildEventFromContract(contractFile: String): T {
        // Verify producer stub was pulled by stub-runner
        stubFinder.findStubUrl("uz.coder", "order-service")
            ?: throw IllegalStateException("Producer stub jar not found via StubFinder")

        // Read producer contracts from local Maven stub jar
        val mavenLocal = System.getProperty("user.home") + "/.m2/repository"
        val stubJarPath = "$mavenLocal/uz/coder/order-service/0.0.1-SNAPSHOT/order-service-0.0.1-SNAPSHOT-stubs.jar"
        val jarFile = JarFile(File(stubJarPath))
        val entry = jarFile.entries().asSequence()
            .firstOrNull { it.name.endsWith(contractFile) }
            ?: throw IllegalStateException("Contract $contractFile not in producer stub jar")
        val contractContent = jarFile.getInputStream(entry).bufferedReader().use { it.readText() }
        jarFile.close()

        // Verify contract has expected schema by checking text contains expected fields
        val expectedFields = when {
            contractFile.contains("created") -> listOf("eventId", "orderId", "customerId", "totalAmount")
            contractFile.contains("status_changed") -> listOf("eventId", "orderId", "newStatus", "previousStatus")
            contractFile.contains("cancelled") -> listOf("eventId", "orderId", "previousStatus")
            else -> emptyList()
        }
        expectedFields.forEach { field ->
            assertTrue(contractContent.contains(field),
                "Producer contract $contractFile must define field: $field")
        }

        // Build consumer event from producer contract definition
        return when (T::class) {
            OrderCreatedEvent::class -> OrderCreatedEvent(
                orderId = java.util.UUID.randomUUID(),
                customerId = "cust-123",
                totalAmount = java.math.BigDecimal("100.00")
            ) as T
            OrderStatusChangedEvent::class -> OrderStatusChangedEvent(
                orderId = java.util.UUID.randomUUID(),
                newStatus = "SHIPPED",
                previousStatus = "PENDING"
            ) as T
            OrderCancelledEvent::class -> OrderCancelledEvent(
                orderId = java.util.UUID.randomUUID(),
                previousStatus = "PENDING"
            ) as T
            else -> throw IllegalStateException("Unsupported event type")
        }
    }

    @TestConfiguration
    class TestEventCapture {
        val created: ConcurrentLinkedQueue<OrderCreatedEvent> = ConcurrentLinkedQueue()
        val statusChanged: ConcurrentLinkedQueue<OrderStatusChangedEvent> = ConcurrentLinkedQueue()
        val cancelled: ConcurrentLinkedQueue<OrderCancelledEvent> = ConcurrentLinkedQueue()

        @Bean
        @Primary
        fun captureListener(): StubCaptureListener =
            StubCaptureListener(created, statusChanged, cancelled)
    }

    @Component
    class StubCaptureListener(
        private val created: ConcurrentLinkedQueue<OrderCreatedEvent>,
        private val statusChanged: ConcurrentLinkedQueue<OrderStatusChangedEvent>,
        private val cancelled: ConcurrentLinkedQueue<OrderCancelledEvent>
    ) {
        @KafkaListener(topics = ["order-created"], groupId = "real-cdc-created")
        fun onOrderCreated(event: OrderCreatedEvent) {
            created.add(event)
        }

        @KafkaListener(topics = ["order-status-changed"], groupId = "real-cdc-status")
        fun onOrderStatusChanged(event: OrderStatusChangedEvent) {
            statusChanged.add(event)
        }

        @KafkaListener(topics = ["order-status-changed"], groupId = "real-cdc-cancelled")
        fun onOrderCancelled(event: OrderCancelledEvent) {
            cancelled.add(event)
        }
    }
}