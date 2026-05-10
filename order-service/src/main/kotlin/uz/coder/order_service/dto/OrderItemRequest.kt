package uz.coder.order_service.dto

import java.math.BigDecimal

data class OrderItemRequest(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal
)