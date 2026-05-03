package uz.coder.order_service.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class OrderRequest(
    @field:NotBlank(message = "Customer ID is required")
    val customerId: String,

    @field:NotEmpty(message = "Order must have at least one item")
    @field:Valid
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,

    @field:NotBlank(message = "Product name is required")
    val productName: String,

    @field:Positive(message = "Quantity must be positive")
    val quantity: Int,

    @field:DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    val unitPrice: BigDecimal
)