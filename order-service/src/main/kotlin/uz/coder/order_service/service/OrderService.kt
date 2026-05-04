package uz.coder.order_service.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uz.coder.order_service.domain.Order
import uz.coder.order_service.domain.OrderItem
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.dto.OrderResponse
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.exception.OrderNotFoundException
import uz.coder.order_service.repository.OrderRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderService(private val orderRepository: OrderRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun findById(id: UUID): OrderResponse {
        log.info("Finding order id={}", id)
        return orderRepository.findById(id)
            .map { OrderResponse.from(it) }
            .orElseThrow { OrderNotFoundException("Order not found: $id") }
    }

    fun findByCustomerId(customerId: String): List<OrderResponse> {
        log.info("Finding orders for customerId={}", customerId)
        return orderRepository.findByCustomerId(customerId).map { OrderResponse.from(it) }
    }

    @Transactional
    fun createOrder(request: OrderRequest): OrderResponse {
        log.info("Creating order customerId={}", request.customerId)
        val order = Order(customerId = request.customerId)
        val items = request.items.map { req ->
            OrderItem(
                order = order,
                productId = req.productId,
                productName = req.productName,
                quantity = req.quantity,
                unitPrice = req.unitPrice
            )
        }
        order.items.addAll(items)
        order.totalAmount = items.sumOf { it.unitPrice.multiply(BigDecimal(it.quantity)) }
        val saved = orderRepository.save(order)
        log.info("Order created id={}", saved.id)
        return OrderResponse.from(saved)
    }

    @Transactional
    fun updateStatus(id: UUID, status: OrderStatus): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { OrderNotFoundException("Order not found: $id") }
        order.status = status
        order.updatedAt = Instant.now()
        return OrderResponse.from(orderRepository.save(order))
    }

    @Transactional
    fun cancelOrder(id: UUID): OrderResponse = updateStatus(id, OrderStatus.CANCELLED)
}