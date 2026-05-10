package uz.coder.order_service.domain

import jakarta.persistence.*
import io.hypersistence.utils.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import uz.coder.order_service.event.OrderEventType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_events")
class OrderEventRecord(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: OrderEventType,

    @Type(JsonType::class)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(name = "sequence_number", insertable = false, updatable = false)
    val sequenceNumber: Long = 0L,

    @Column(name = "occurred_at", nullable = false, updatable = false)
    val occurredAt: Instant = Instant.now(),

    @Column(name = "published", nullable = false)
    var published: Boolean = false,

    @Column(name = "published_at")
    var publishedAt: Instant? = null
)