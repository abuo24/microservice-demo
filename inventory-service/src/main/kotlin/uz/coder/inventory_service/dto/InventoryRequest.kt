package uz.coder.inventory_service.dto

import java.math.BigDecimal

data class InventoryRequest(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal
)

data class ReserveRequest(
    val productId: String,
    val quantity: Int
)