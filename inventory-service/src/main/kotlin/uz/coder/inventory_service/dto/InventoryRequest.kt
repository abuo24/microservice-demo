package uz.coder.inventory_service.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class InventoryRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,

    @field:NotBlank(message = "Product name is required")
    val productName: String,

    @field:PositiveOrZero(message = "Quantity must be zero or positive")
    val quantity: Int,

    @field:DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    val unitPrice: BigDecimal
)

data class ReserveRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,

    @field:PositiveOrZero(message = "Quantity must be positive")
    val quantity: Int
)