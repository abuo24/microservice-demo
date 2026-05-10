package uz.coder.order_service.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uz.coder.order_service.audit.Auditable
import uz.coder.order_service.domain.Order
import uz.coder.order_service.domain.OrderItem
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.dto.OrderResponse
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.exception.OrderNotFoundException
import uz.coder.order_service.event.OrderEventPublisher
import uz.coder.order_service.repository.OrderRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val meterRegistry: MeterRegistry,
    private val eventPublisher: OrderEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val ordersCreated = Counter.builder("orders.created.total")
        .description("Total orders created")
        .register(meterRegistry)

    private val ordersCancelled = Counter.builder("orders.cancelled.total")
        .description("Total orders cancelled")
        .register(meterRegistry)

    private val orderCreationTimer = Timer.builder("orders.creation.duration")
        .description("Time to create an order")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry)

    @Cacheable(cacheNames = ["orders"], key = "#id")
    fun findById(id: UUID): OrderResponse {
        log.info("Finding order id={}", id)
        return orderRepository.findById(id)
            .map { OrderResponse.from(it) }
            .orElseThrow { OrderNotFoundException("Order not found: $id") }
    }

    @Cacheable(cacheNames = ["orders"], key = "#customerId")
    fun findByCustomerId(customerId: String): List<OrderResponse> {
        log.info("Finding orders for customerId={}", customerId)
        return orderRepository.findByCustomerId(customerId).map { OrderResponse.from(it) }
    }

    @Auditable(action = "CREATE_ORDER", resourceType = "ORDER")
    @Transactional
    fun createOrder(request: OrderRequest): OrderResponse {
        log.info("Creating order customerId={}", request.customerId)
        return orderCreationTimer.recordCallable {
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
            ordersCreated.increment()
            log.info("Order created id={}", saved.id)
            eventPublisher.publishOrderCreated(
                uz.coder.order_service.event.OrderCreatedEvent(
                    orderId = saved.id!!,
                    customerId = saved.customerId,
                    totalAmount = saved.totalAmount
                )
            )
            OrderResponse.from(saved)
        }!!
    }

    @Auditable(action = "UPDATE_ORDER_STATUS", resourceType = "ORDER")
    @Transactional
    fun updateStatus(id: UUID, status: OrderStatus): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { OrderNotFoundException("Order not found: $id") }
        val previousStatus = order.status.name
        order.status = status
        order.updatedAt = Instant.now()
        val saved = orderRepository.save(order)
        eventPublisher.publishOrderStatusChanged(
            uz.coder.order_service.event.OrderStatusChangedEvent(
                orderId = saved.id!!,
                newStatus = status.name,
                previousStatus = previousStatus
            )
        )
        return OrderResponse.from(saved)
    }

    @Auditable(action = "CANCEL_ORDER", resourceType = "ORDER")
    @Transactional
    fun cancelOrder(id: UUID): OrderResponse {
        ordersCancelled.increment()
        val order = orderRepository.findById(id)
            .orElseThrow { OrderNotFoundException("Order not found: $id") }
        val previousStatus = order.status.name
        order.status = OrderStatus.CANCELLED
        order.updatedAt = Instant.now()
        val saved = orderRepository.save(order)
        eventPublisher.publishOrderStatusChanged(
            uz.coder.order_service.event.OrderStatusChangedEvent(
                orderId = saved.id!!,
                newStatus = OrderStatus.CANCELLED.name,
                previousStatus = previousStatus
            )
        )
        return OrderResponse.from(saved)
    }
}