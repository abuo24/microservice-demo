package uz.coder.order_service.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import uz.coder.order_service.event.OrderCancelledEvent
import uz.coder.order_service.event.OrderCreatedEvent
import uz.coder.order_service.event.OrderEvent
import uz.coder.order_service.event.OrderStatusChangedEvent
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["order-created", "order-status-changed"])
class ProducerCdcTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderEvent>

    @Autowired
    private lateinit var embeddedKafka: EmbeddedKafkaBroker

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun createConsumer(group: String, topic: String): Consumer<String, String> {
        val props = KafkaTestUtils.consumerProps(group, "true", embeddedKafka)
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(topic))
        consumer.poll(Duration.ofMillis(100))
        return consumer
    }

    @Test
    fun `OrderCreatedEvent published to order-created topic matches consumer contract`() {
        val consumer = createConsumer("cdc-created", "order-created")

        val event = OrderCreatedEvent(
            orderId = UUID.randomUUID(),
            customerId = "cust-cdc-1",
            totalAmount = BigDecimal("100.00")
        )
        kafkaTemplate.send("order-created", event.orderId.toString(), event).get()

        val records = pollUntilMessage(consumer)
        consumer.close()

        assertTrue(records.count() > 0, "Producer must publish to order-created topic")
        val parsed = objectMapper.readTree(records.iterator().next().value())

        assertContractCompliance(parsed, mapOf(
            "eventId" to "uuid",
            "timestamp" to "isoDateTime",
            "orderId" to "uuid",
            "customerId" to "string",
            "totalAmount" to "decimal"
        ))
        assertEquals("cust-cdc-1", parsed["customerId"].asText())
        assertEquals(0, BigDecimal("100.00").compareTo(parsed["totalAmount"].decimalValue()))
    }

    @Test
    fun `OrderStatusChangedEvent published to order-status-changed topic matches consumer contract`() {
        val consumer = createConsumer("cdc-status", "order-status-changed")

        val event = OrderStatusChangedEvent(
            orderId = UUID.randomUUID(),
            newStatus = "SHIPPED",
            previousStatus = "PENDING"
        )
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event).get()

        val records = pollUntilMessage(consumer)
        consumer.close()

        assertTrue(records.count() > 0)
        val parsed = objectMapper.readTree(records.iterator().next().value())

        assertContractCompliance(parsed, mapOf(
            "eventId" to "uuid",
            "timestamp" to "isoDateTime",
            "orderId" to "uuid",
            "newStatus" to "string",
            "previousStatus" to "string"
        ))
        assertEquals("SHIPPED", parsed["newStatus"].asText())
        assertEquals("PENDING", parsed["previousStatus"].asText())
    }

    @Test
    fun `OrderCancelledEvent published to order-status-changed topic matches consumer contract`() {
        val consumer = createConsumer("cdc-cancelled", "order-status-changed")

        val event = OrderCancelledEvent(
            orderId = UUID.randomUUID(),
            previousStatus = "PENDING"
        )
        kafkaTemplate.send("order-status-changed", event.orderId.toString(), event).get()

        val records = pollUntilMessage(consumer)
        consumer.close()

        assertTrue(records.count() > 0)
        val parsed = objectMapper.readTree(records.iterator().next().value())

        assertContractCompliance(parsed, mapOf(
            "eventId" to "uuid",
            "timestamp" to "isoDateTime",
            "orderId" to "uuid",
            "previousStatus" to "string"
        ))
        assertEquals("PENDING", parsed["previousStatus"].asText())
    }

    private fun pollUntilMessage(consumer: Consumer<String, String>): ConsumerRecords<String, String> {
        var attempts = 0
        while (attempts < 30) {
            val records = consumer.poll(Duration.ofMillis(500))
            if (records.count() > 0) return records
            attempts++
        }
        return ConsumerRecords.empty()
    }

    private fun assertContractCompliance(json: JsonNode, schema: Map<String, String>) {
        schema.forEach { (field, type) ->
            assertTrue(json.has(field), "Contract requires field: $field")
            val value = json[field]
            when (type) {
                "uuid" -> UUID.fromString(value.asText())
                "isoDateTime" -> assertTrue(value.isNumber || value.isTextual, "timestamp must be number or string")
                "string" -> assertTrue(value.isTextual)
                "decimal" -> assertTrue(value.isNumber)
            }
        }
    }
}