package uz.coder.order_service.graphql

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import uz.coder.order_service.dto.OrderResponse
import uz.coder.order_service.service.OrderService
import java.util.UUID

@Controller
class OrderGraphQL(private val orderService: OrderService) {

    @QueryMapping
    fun orderById(@Argument id: String): OrderResponse =
        orderService.findById(UUID.fromString(id))

    @QueryMapping
    fun ordersByCustomerId(@Argument customerId: String): List<OrderResponse> =
        orderService.findByCustomerId(customerId)

    @QueryMapping
    fun allOrders(): List<OrderResponse> =
        emptyList()
}