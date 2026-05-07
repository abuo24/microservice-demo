package uz.coder.order_service.dto

import java.math.BigDecimal

data class OrderRequest(
    val customerId: String,
    val items: List<OrderItemRequest>
)