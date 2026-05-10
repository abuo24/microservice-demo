package uz.coder.order_service.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uz.coder.order_service.domain.OrderEventRecord
import java.util.UUID

@Repository
interface EventStoreRepository : JpaRepository<OrderEventRecord, UUID> {

    fun findByAggregateIdOrderBySequenceNumberAsc(aggregateId: UUID): List<OrderEventRecord>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e FROM OrderEventRecord e
        WHERE e.published = false
        ORDER BY e.sequenceNumber ASC
    """)
    fun findUnpublishedEvents(): List<OrderEventRecord>
}