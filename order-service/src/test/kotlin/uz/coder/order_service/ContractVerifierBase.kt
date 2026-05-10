package uz.coder.order_service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import uz.coder.order_service.dto.OrderItemRequest
import uz.coder.order_service.dto.OrderRequest
import uz.coder.order_service.enumuration.OrderStatus
import uz.coder.order_service.service.OrderService
import java.math.BigDecimal

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = ["log.segment.bytes=1048576"])
abstract class ContractVerifierBase {

    @Autowired
    private lateinit var orderService: OrderService

    fun createOrderAndPublishEvent() {
        val request = OrderRequest(
            customerId = "cust-123",
            items = listOf(OrderItemRequest("prod-1", "Product 1", 1, BigDecimal("100.00")))
        )
        orderService.createOrder(request)
        Thread.sleep(500)
    }

    fun updateOrderStatusAndPublishEvent() {
        val request = OrderRequest(
            customerId = "cust-456",
            items = listOf(OrderItemRequest("prod-2", "Product 2", 1, BigDecimal("50.00")))
        )
        val created = orderService.createOrder(request)
        Thread.sleep(500)
        orderService.updateStatus(created.id!!, OrderStatus.SHIPPED)
        Thread.sleep(500)
    }

    fun cancelOrderAndPublishEvent() {
        val request = OrderRequest(
            customerId = "cust-789",
            items = listOf(OrderItemRequest("prod-3", "Product 3", 1, BigDecimal("75.00")))
        )
        val created = orderService.createOrder(request)
        Thread.sleep(500)
        orderService.cancelOrder(created.id!!)
        Thread.sleep(500)
    }
}