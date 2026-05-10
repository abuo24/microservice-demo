package uz.coder.inventory_service.contract

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import uz.coder.inventory_service.event.OrderCancelledEvent
import uz.coder.inventory_service.event.OrderCreatedEvent
import uz.coder.inventory_service.event.OrderStatusChangedEvent
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrderEventContractTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @Test
    fun `Consumer expects OrderCreatedEvent with required fields from producer`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "customerId": "cust-123",
                "totalAmount": 100.00
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCreatedEvent::class.java)

        assertNotNull(event.eventId)
        assertNotNull(event.timestamp)
        assertNotNull(event.orderId)
        assertEquals("cust-123", event.customerId)
        assertEquals(BigDecimal("100.00"), event.totalAmount)
    }

    @Test
    fun `Consumer expects OrderStatusChangedEvent with required fields from producer`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "newStatus": "SHIPPED",
                "previousStatus": "PENDING"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderStatusChangedEvent::class.java)

        assertEquals("SHIPPED", event.newStatus)
        assertEquals("PENDING", event.previousStatus)
    }

    @Test
    fun `Consumer expects OrderCancelledEvent with required fields from producer`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "previousStatus": "PENDING"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCancelledEvent::class.java)

        assertEquals("PENDING", event.previousStatus)
        assertEquals(null, event.reason)
    }

    @Test
    fun `Consumer handles OrderCancelledEvent with optional reason field`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "previousStatus": "PENDING",
                "reason": "Customer changed mind"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCancelledEvent::class.java)

        assertEquals("Customer changed mind", event.reason)
    }

    @Test
    fun `Consumer can ignore unknown fields from producer payload`() {
        val producerJson = """
            {
                "eventId": "${UUID.randomUUID()}",
                "timestamp": "${Instant.now()}",
                "orderId": "${UUID.randomUUID()}",
                "customerId": "cust-789",
                "totalAmount": 50.00,
                "unknownField": "should be ignored"
            }
        """.trimIndent()

        val event = objectMapper.readValue(producerJson, OrderCreatedEvent::class.java)
        assertEquals("cust-789", event.customerId)
    }
}