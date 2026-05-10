package uz.coder.order_service.domain

import jakarta.persistence.*
import io.hypersistence.utils.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import uz.coder.order_service.enumuration.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_projections")
class OrderProjection(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,

    @Column(name = "customer_id", nullable = false)
    var customerId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "item_count", nullable = false)
    var itemCount: Int = 0,

    @Column(name = "last_event_sequence", nullable = false)
    var lastEventSequence: Long = 0L,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)