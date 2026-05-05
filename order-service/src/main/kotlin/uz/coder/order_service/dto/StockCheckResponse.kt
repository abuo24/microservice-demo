package uz.coder.order_service.dto

data class StockCheckResponse(
    val productId: String,
    val productName: String,
    val availableQuantity: Int,
    val available: Boolean,
    val fallback: Boolean = false,
    val fallbackReason: String? = null
)