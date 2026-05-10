package uz.coder.order_service.contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import uz.coder.order_service.event.OrderCancelledEvent
import uz.coder.order_service.event.OrderCreatedEvent
import uz.coder.order_service.event.OrderStatusChangedEvent
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrderEventContractTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `OrderCreatedEvent contract - producer publishes expected schema`() {
        val orderId = UUID.randomUUID()
        val event = OrderCreatedEvent(
            orderId = orderId,
            customerId = "cust-123",
            totalAmount = BigDecimal("100.00")
        )

        val json = objectMapper.writeValueAsString(event)
        val parsed = objectMapper.readTree(json)

        assertTrue(parsed.has("eventId"), "Must have eventId field")
        assertTrue(parsed.has("timestamp"), "Must have timestamp field")
        assertTrue(parsed.has("orderId"), "Must have orderId field")
        assertTrue(parsed.has("customerId"), "Must have customerId field")
        assertTrue(parsed.has("totalAmount"), "Must have totalAmount field")

        assertNotNull(UUID.fromString(parsed["eventId"].asText()))
        assertNotNull(Instant.parse(parsed["timestamp"].asText()))
        assertEquals(orderId.toString(), parsed["orderId"].asText())
        assertEquals("cust-123", parsed["customerId"].asText())
        assertEquals(0, BigDecimal("100.00").compareTo(parsed["totalAmount"].decimalValue()))
    }

    @Test
    fun `OrderCreatedEvent contract - consumer can deserialize producer payload`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "customerId": "cust-456",
                "totalAmount": 250.50
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCreatedEvent::class.java)

        assertNotNull(event.eventId)
        assertNotNull(event.timestamp)
        assertNotNull(event.orderId)
        assertEquals("cust-456", event.customerId)
        assertEquals(BigDecimal("250.50"), event.totalAmount)
    }

    @Test
    fun `OrderStatusChangedEvent contract - producer publishes expected schema`() {
        val orderId = UUID.randomUUID()
        val event = OrderStatusChangedEvent(
            orderId = orderId,
            newStatus = "SHIPPED",
            previousStatus = "PENDING"
        )

        val json = objectMapper.writeValueAsString(event)
        val parsed = objectMapper.readTree(json)

        assertTrue(parsed.has("eventId"))
        assertTrue(parsed.has("timestamp"))
        assertTrue(parsed.has("orderId"))
        assertTrue(parsed.has("newStatus"))
        assertTrue(parsed.has("previousStatus"))

        assertEquals("SHIPPED", parsed["newStatus"].asText())
        assertEquals("PENDING", parsed["previousStatus"].asText())
    }

    @Test
    fun `OrderStatusChangedEvent contract - consumer can deserialize producer payload`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "newStatus": "DELIVERED",
                "previousStatus": "SHIPPED"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderStatusChangedEvent::class.java)

        assertEquals("DELIVERED", event.newStatus)
        assertEquals("SHIPPED", event.previousStatus)
    }

    @Test
    fun `OrderCancelledEvent contract - producer publishes expected schema`() {
        val orderId = UUID.randomUUID()
        val event = OrderCancelledEvent(
            orderId = orderId,
            previousStatus = "PENDING"
        )

        val json = objectMapper.writeValueAsString(event)
        val parsed = objectMapper.readTree(json)

        assertTrue(parsed.has("eventId"))
        assertTrue(parsed.has("timestamp"))
        assertTrue(parsed.has("orderId"))
        assertTrue(parsed.has("previousStatus"))

        assertEquals("PENDING", parsed["previousStatus"].asText())
    }

    @Test
    fun `OrderCancelledEvent contract - consumer can deserialize producer payload`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "previousStatus": "PENDING",
                "reason": "Customer request"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCancelledEvent::class.java)

        assertEquals("PENDING", event.previousStatus)
        assertEquals("Customer request", event.reason)
    }

    @Test
    fun `OrderCancelledEvent contract - reason field is optional`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "previousStatus": "PENDING"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCancelledEvent::class.java)
        assertEquals(null, event.reason)
    }
}