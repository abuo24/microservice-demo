package uz.coder.order_service.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uz.coder.order_service.domain.OrderProjection
import java.util.UUID

@Repository
interface OrderProjectionRepository : JpaRepository<OrderProjection, UUID> {
    fun findByAggregateId(aggregateId: UUID): OrderProjection?
    fun findByCustomerId(customerId: String): List<OrderProjection>
}