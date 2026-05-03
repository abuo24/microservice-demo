package uz.coder.order_service.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uz.coder.order_service.domain.Order
import uz.coder.order_service.enumuration.OrderStatus
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByCustomerId(customerId: String): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.status = :status")
    fun findByCustomerIdAndStatus(customerId: String, status: OrderStatus): List<Order>
}